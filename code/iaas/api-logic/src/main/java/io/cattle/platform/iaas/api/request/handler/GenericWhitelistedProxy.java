package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.iaas.api.servlet.filter.ProxyPreFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.protocol.HTTP;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;

public class GenericWhitelistedProxy extends AbstractResponseGenerator {

    public static final String ALLOWED_HOST = GenericWhitelistedProxy.class.getName() + "allowed.host";
    public static final String SET_HOST_CURRENT_HOST = GenericWhitelistedProxy.class.getName() + "set_host_current_host";
    public static final String REDIRECTS = GenericWhitelistedProxy.class.getName() + "redirects";

    private static final DynamicBooleanProperty ALLOW_PROXY = ArchaiusUtil.getBoolean("api.proxy.allow");
    private static final DynamicStringListProperty PROXY_WHITELIST = ArchaiusUtil.getList("api.proxy.whitelist");

    private static final String FORWARD_PROTO = "X-Forwarded-Proto";
    private static final String API_AUTH = "X-API-AUTH-HEADER";
    private static final Set<String> BAD_HEADERS = new HashSet<>(Arrays.asList(HTTP.TARGET_HOST.toLowerCase(), "authorization",
            HTTP.TRANSFER_ENCODING.toLowerCase(), HTTP.CONTENT_LEN.toLowerCase(), API_AUTH.toLowerCase()));

    private static final Executor EXECUTOR;
    private static final Executor NO_REDIRECT_EXECUTOR;

    static {
        LayeredConnectionSocketFactory ssl = null;
        try {
            ssl = SSLConnectionSocketFactory.getSystemSocketFactory();
        } catch (final SSLInitializationException ex) {
            final SSLContext sslcontext;
            try {
                sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                sslcontext.init(null, null, null);
                ssl = new SSLConnectionSocketFactory(sslcontext);
            } catch (final SecurityException ignore) {
            } catch (final KeyManagementException ignore) {
            } catch (final NoSuchAlgorithmException ignore) {
            }
        }

        final Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", ssl != null ? ssl : SSLConnectionSocketFactory.getSocketFactory())
            .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(sfr);
        cm.setDefaultMaxPerRoute(100);
        cm.setMaxTotal(200);
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .build();
        HttpClient noRdhttpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT).setRedirectsEnabled(false).build())
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .build();
        EXECUTOR = Executor.newInstance(httpClient);
        NO_REDIRECT_EXECUTOR = Executor.newInstance(noRdhttpClient);
    }

    Cache<String, Boolean> allowCache = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    @Inject
    ObjectManager objectManager;

    protected boolean isAllowed(HttpServletRequest servletRequest, String host) {
        boolean allowHost = Boolean.TRUE.equals(servletRequest.getAttribute(ALLOWED_HOST));
        if (allowHost) {
            return true;
        }

        if (isWhitelisted(host)) {
            return true;
        }

        Boolean value = allowCache.getIfPresent(host);
        if (value == null) {
            List<MachineDriver> drivers = objectManager.find(MachineDriver.class, ObjectMetaDataManager.STATE_FIELD,
                    new Condition(ConditionType.NE, CommonStatesConstants.PURGED));
            for (MachineDriver driver : drivers) {
                String url = DataAccessor.fieldString(driver, "uiUrl");
                if (url != null) {
                    try {
                        URL parsed = new URL(url);
                        allowCache.put(parsed.getHost(), true);
                    } catch (MalformedURLException e) {
                    }
                }
            }
        }
        value = allowCache.getIfPresent(host);
        return value == null ? false : value;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (request.getVersion() == null || !ALLOW_PROXY.get())
            return;

        if (!"proxy".equals(request.getType())) {
            return;
        }

        HttpServletRequest servletRequest = request.getServletContext().getRequest();
        boolean setCurrentHost = Boolean.TRUE.equals(servletRequest.getAttribute(SET_HOST_CURRENT_HOST));
        boolean redirects = !Boolean.FALSE.equals(servletRequest.getAttribute(REDIRECTS));

        String redirect = servletRequest.getRequestURI();
        redirect = StringUtils.substringAfter(redirect, "/proxy/");
        if (redirect.startsWith("http")) {
            /* We don't allow // so http:// will be http:/ and same with https. So we fixup here */
            redirect = redirect.replaceFirst("^http:/([^/])", "http://$1");
            redirect = redirect.replaceFirst("^https:/([^/])", "https://$1");
        }

        if (!StringUtils.startsWith(redirect, "http")) {
            redirect = "https://" + redirect;
        }

        URIBuilder uri;
        try {
            uri = new URIBuilder(redirect);
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid/empty", null);
        }
        String queryInfo = servletRequest.getQueryString();
        if (queryInfo != null) {
            uri.setCustomQuery(URLDecoder.decode(queryInfo, "UTF-8"));
        }
        try {
            redirect = uri.build().toString();
        } catch (URISyntaxException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidRedirect", "The redirect is invalid", null);
        }

        String host = uri.getPort() > 0 ? String.format("%s:%s", uri.getHost(), uri.getPort()) : uri.getHost();

        if (!isAllowed(servletRequest, host)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        Request temp;
        String method = servletRequest.getMethod();
        if (servletRequest instanceof ProxyPreFilter.Request) {
            method = ((ProxyPreFilter.Request)servletRequest).getRealMethod();
        }

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

        // This isn't always available. As is the case for proxy protocol
        String xForwardedProto = servletRequest.getHeader(FORWARD_PROTO);
        if (xForwardedProto == null && request.getRequestUrl() != null && request.getRequestUrl().startsWith("https")) {
            temp.addHeader(FORWARD_PROTO, "https");
        }

        for (String headerName : (List<String>)Collections.list(servletRequest.getHeaderNames())) {
            if (BAD_HEADERS.contains(headerName.toLowerCase())) {
                continue;
            }
            for (String headerVal : (List<String>)Collections.list(servletRequest.getHeaders(headerName))) {
                temp.addHeader(headerName, StringUtils.removeStart(headerVal, "rancher:"));
            }
        }

        String authHeader = servletRequest.getHeader(API_AUTH);
        if (authHeader != null) {
            temp.addHeader("Authorization", authHeader);
        }

        if (setCurrentHost) {
            temp.addHeader("Host", request.getResponseUrlBase().replaceFirst("^https?://", ""));
        } else {
            temp.addHeader("Host", host);
        }

        if ("POST".equals(method) || "PUT".equals(method)) {
            int length = servletRequest.getContentLength();
            InputStreamEntity entity = new InputStreamEntity(request.getInputStream(), length);
            temp.body(entity);
        }

        Response res = redirects ? EXECUTOR.execute(temp) : NO_REDIRECT_EXECUTOR.execute(temp);

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
                    request.getServletContext().getResponse().setHeader(headers[i].getName(), headers[i].getValue());
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
        for (String valid : PROXY_WHITELIST.get()) {
            if (valid.equals(host)) {
                return true;
            }

            if (valid.startsWith("*") && host.endsWith(valid.substring(1))) {
                return true;
            }
        }

        return false;
    }

}
