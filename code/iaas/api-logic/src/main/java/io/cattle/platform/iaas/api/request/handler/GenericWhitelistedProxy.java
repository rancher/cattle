package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringListProperty;

public class GenericWhitelistedProxy extends AbstractResponseGenerator {

    public static final String ALLOWED_HOST = GenericWhitelistedProxy.class.getName() + "allowed.host";
    public static final String SET_HOST_CURRENT_HOST = GenericWhitelistedProxy.class.getName() + "set_host_current_host";

    private static final DynamicBooleanProperty ALLOW_PROXY = ArchaiusUtil.getBoolean("api.proxy.allow");
    private static final DynamicStringListProperty PROXY_WHITELIST = ArchaiusUtil.getList("api.proxy.whitelist");
    private static final DynamicIntProperty MAX_CONTENT_LENGTH = ArchaiusUtil.getInt("api.proxy.max-content-length");

    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (request.getRequestVersion() == null || !ALLOW_PROXY.get())
            return;

        if (!"proxy".equals(request.getType())) {
            return;
        }

        HttpServletRequest servletRequest = request.getServletContext().getRequest();
        boolean allowHost = Boolean.TRUE.equals(servletRequest.getAttribute(ALLOWED_HOST));
        boolean setCurrentHost = Boolean.TRUE.equals(servletRequest.getAttribute(SET_HOST_CURRENT_HOST));

        String redirect = servletRequest.getRequestURI();
        redirect = StringUtils.substringAfter(redirect, "/proxy/");
        redirect = URLDecoder.decode(redirect, "UTF-8");
        if (redirect.startsWith("http")) {
            /* We don't allow // so http:// will be http:/ and same with https. So we fixup here */
            redirect = redirect.replaceFirst("^http:/([^/])", "http://$1");
            redirect = redirect.replaceFirst("^https:/([^/])", "https://$1");
        }

        URIBuilder uri;
        try {
            uri = new URIBuilder(redirect);
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid/empty", null);
        }
        String queryInfo = servletRequest.getQueryString();
        uri.setCustomQuery(queryInfo);
        try {
            redirect = uri.build().toString();
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid", null);
        }

        if (!StringUtils.startsWith(redirect, "http")) {
            redirect = "https://" + redirect;
        }

        String host = null;
        try {
            host = new URL(redirect).getHost();
        } catch (MalformedURLException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid", null);
        }

        if (!allowHost && !isWhitelisted(StringUtils.strip(host, "/"))) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        Request temp;
        String method = servletRequest.getMethod();
        switch (method) {
        case "POST":
            temp = Request.Post(redirect);
            break;
        case "GET":
            temp = Request.Get(redirect);
            break;
        case "PUT":
            temp = Request.Put(redirect);
            break;
        case "DELETE":
            temp = Request.Delete(redirect);
            break;
        case "HEAD":
            temp = Request.Head(redirect);
            break;
        default:
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "Invalid method", "The method " + method + " is not supported", null);
        }

        @SuppressWarnings("unchecked")
        List<String> restrictedHeaders = Collections.list(servletRequest.getHeaders("X-API-HEADERS-RESTRICT"));

        if (restrictedHeaders.size() == 1 && StringUtils.contains(restrictedHeaders.get(0), ",")) {
            String[] headers = StringUtils.split(restrictedHeaders.get(0), ",");
            restrictedHeaders.remove(0);
            for (String header : headers) {
                restrictedHeaders.add(StringUtils.trim(header));
            }
        }

        restrictedHeaders.add("Host");
        restrictedHeaders.add("Authorization");

        @SuppressWarnings("unchecked")
        List<String> headerNames = Collections.list(servletRequest.getHeaderNames());

        for (String headerName : headerNames) {
            if (restrictedHeaders.contains(headerName)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Enumeration<String> headerVals = servletRequest.getHeaders(headerName);
            String headerKey = headerName;
            if (StringUtils.equalsIgnoreCase("X-API-AUTH-HEADER", headerName)) {
                headerKey = "Authorization";
            }
            while (headerVals.hasMoreElements()) {
                String headerVal = headerVals.nextElement();
                if (StringUtils.startsWith(headerVal, "rancher:")) {
                    headerVal = StringUtils.substringAfter(headerVal, "rancher:");
                }
                temp.addHeader(headerKey, headerVal);
            }
        }

        if (setCurrentHost) {
            temp.addHeader("Host", request.getResponseUrlBase().replaceFirst("^https?://", ""));
        } else {
            temp.addHeader("Host", host);
        }

        if ("POST".equals(method) || "PUT".equals(method)) {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                final InputStream finalStream = inputStream;
                LimitedInputStream istream = new LimitedInputStream(finalStream, MAX_CONTENT_LENGTH.get()) {
                    @Override
                    protected void raiseError(long pSizeMax, long pCount) throws IOException {
                        finalStream.close();
                        throw new IOException("content-length " + pCount + " exceeded max-length of " + pSizeMax);
                    }
                };
                temp.bodyByteArray(IOUtils.toByteArray(istream));
            }
        }

        Response res = temp.execute();

        res.handleResponse(new ResponseHandler<Object>() {
            @Override
            public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                int statusCode = response.getStatusLine().getStatusCode();
                request.setResponseObject(new Object());
                request.setResponseCode(statusCode);
                request.commit();
                OutputStream writer = request.getServletContext().getResponse().getOutputStream();
                Header[] headers = response.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    request.getServletContext().getResponse().addHeader(headers[i].getName(), headers[i].getValue());
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    entity.writeTo(writer);
                }
                return null;
            }
        });
    }

    private boolean isWhitelisted(String host) {
        return PROXY_WHITELIST.get().contains(host);
    }

}
