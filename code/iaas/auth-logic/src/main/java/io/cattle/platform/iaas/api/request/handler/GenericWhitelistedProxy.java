package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GenericWhitelistedProxy extends AbstractResponseGenerator {

    private static final String AUTH_HEADER = "Authorization";

    private static final DynamicBooleanProperty ALLOW_PROXY = ArchaiusUtil.getBoolean("api.proxy.allow");
    private static final DynamicStringProperty PROXY_WHITELIST = ArchaiusUtil.getString("api.proxy.whitelist");
    private Set<String> whitelist;

    GithubUtils githubUtils;

    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (request.getRequestVersion() == null || !ALLOW_PROXY.get())
            return;

        if (!StringUtils.equals("proxy", request.getRequestVersion())) {
            return;
        }

        String token = request.getServletContext().getRequest().getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
        }
        String accessToken = githubUtils.validateAndFetchGithubToken(token);
        if (StringUtils.isEmpty(accessToken)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        String redirect = request.getServletContext().getRequest().getRequestURI();
        redirect = redirect.substring("/proxy/".length());
        URIBuilder uri;
        try {
            uri = new URIBuilder(redirect);
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid/empty", null);
        }
        String queryInfo = request.getServletContext().getRequest().getQueryString();
        uri.setCustomQuery(queryInfo);
        try {
            redirect = uri.build().toString();
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid", null);
        }

        if (!isWhitelisted(redirect)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        Request temp;
        String method = request.getServletContext().getRequest().getMethod();
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
        List<String> headerNames = Collections.list(request.getServletContext().getRequest().getHeaderNames());

        for (String headerName : headerNames) {
            if ("Content-Length".equals(headerName) || "Authorization".equals(headerName) || "Host".equals(headerName)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Enumeration<String> headerVals = request.getServletContext().getRequest().getHeaders(headerName);
            String headerKey = headerName;
            if("X-API-AUTH-HEADER".equals(headerName)) {
                headerKey = "Authorization";
            }
            while (headerVals.hasMoreElements()) {
                temp.addHeader(headerKey, headerVals.nextElement());
            }
        }

        if ("POST".equals(method) || "PUT".equals(method)) {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                temp.bodyStream(inputStream);
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
                response.getEntity().writeTo(writer);
                return null;
            }
        });
    }

    @Inject
    public void setGithubUtils(GithubUtils githubUtils) {
        this.githubUtils = githubUtils;
    }

    private boolean isWhitelisted(String uri) {
        URI url = URI.create(uri);
        String host = url.getHost();
        if (null == whitelist) {
            whitelist = new HashSet<String>();
            String commaSeparatedWhitelist = PROXY_WHITELIST.get();
            String[] whitelistedHostnames = StringUtils.split(commaSeparatedWhitelist, ",");
            for (String hostname : whitelistedHostnames) {
                whitelist.add(hostname);
            }
        }
        return whitelist.contains(host);
    }
}