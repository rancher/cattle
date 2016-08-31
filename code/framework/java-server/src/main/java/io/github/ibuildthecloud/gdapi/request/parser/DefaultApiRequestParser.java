package io.github.ibuildthecloud.gdapi.request.parser;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;

public class DefaultApiRequestParser implements ApiRequestParser {

    public static final String DEFAULT_OVERRIDE_URL_HEADER = "X-API-request-url";
    public static final String DEFAULT_OVERRIDE_CLIENT_IP_HEADER = "X-API-client-ip";
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    public static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    public static final String FORWARDED_PORT_HEADER = "X-Forwarded-Port";
    public static final String HOST_HEADER = "Host";

    public static final String HTML = "html";
    public static final String JSON = "json";

    ServletFileUpload servletFileUpload;
    int maxUploadSize = 100 * 1024;
    boolean allowClientOverrideHeaders = false;
    String overrideUrlHeader = DEFAULT_OVERRIDE_URL_HEADER;
    String overrideClientIpHeader = DEFAULT_OVERRIDE_CLIENT_IP_HEADER;
    Set<String> allowedFormats;
    String trimPrefix;

    @Override
    public boolean parse(ApiRequest apiRequest) throws IOException {
        HttpServletRequest request = apiRequest.getServletContext().getRequest();

        apiRequest.setLocale(getLocale(apiRequest, request));
        apiRequest.setMethod(parseMethod(apiRequest, request));
        apiRequest.setAction(parseAction(apiRequest, request));
        apiRequest.setRequestParams(parseParams(apiRequest, request));
        apiRequest.setRequestUrl(parseRequestUrl(apiRequest, request));
        apiRequest.setClientIp(parseClientIp(apiRequest, request));
        apiRequest.setResponseUrlBase(parseResponseUrlBase(apiRequest, request));
        apiRequest.setVersion(parseVersion(apiRequest, request));
        apiRequest.setResponseFormat(parseResponseType(apiRequest, request));
        apiRequest.setQueryString(parseQueryString(apiRequest, request));

        parsePath(apiRequest, request);

        return true;
    }

    protected Locale getLocale(ApiRequest apiRequest, HttpServletRequest request) {
        return request.getLocale();
    }

    protected String parseQueryString(ApiRequest apiRequest, HttpServletRequest request) {
        return request.getQueryString();
    }

    protected String parseMethod(ApiRequest apiRequest, HttpServletRequest request) {
        String method = request.getParameter("_method");

        if (method == null)
            method = request.getMethod();

        return method;
    }

    protected String parseAction(ApiRequest apiRequest, HttpServletRequest request) {
        if ("POST".equals(apiRequest.getMethod())) {
            return request.getParameter(Resource.ACTION);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseParams(ApiRequest apiRequest, HttpServletRequest request) throws IOException {
        try {
            Map<String, Object> multiPart = parseMultipart(request);

            return multiPart == null ? request.getParameterMap() : multiPart;
        } catch (IOException e) {
            if (e.getCause() instanceof FileUploadBase.SizeLimitExceededException)
                throw new ClientVisibleException(ResponseCodes.REQUEST_ENTITY_TOO_LARGE);
            throw e;
        }
    }

    protected Map<String, Object> parseMultipart(HttpServletRequest request) throws IOException {
        if (!ServletFileUpload.isMultipartContent(request))
            return null;

        Map<String, List<String>> params = new HashMap<String, List<String>>();

        try {
            List<FileItem> items = servletFileUpload.parseRequest(request);

            for (FileItem item : items) {
                if (item.isFormField()) {
                    List<String> values = params.get(item.getFieldName());
                    if (values == null) {
                        values = new ArrayList<String>();
                        params.put(item.getFieldName(), values);
                    }
                    values.add(item.getString());
                }
            }

            Map<String, Object> result = new HashMap<String, Object>();

            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                List<String> values = entry.getValue();
                result.put(entry.getKey(), values.toArray(new String[values.size()]));
            }

            return result;
        } catch (FileUploadException e) {
            throw new IOException(e);
        }
    }

    protected String getOverrideHeader(HttpServletRequest request, String header, String defaultValue) {
        return getOverrideHeader(request, header, defaultValue, true);
    }

    protected String getOverrideHeader(HttpServletRequest request, String header, String defaultValue, boolean checkSetting) {
        if (checkSetting && !isAllowClientOverrideHeaders()) {
            return defaultValue;
        }

        // Need to handle comma separated hosts in X-Forwarded-For
        String value = request.getHeader(header);
        if (value != null) {
            String[] ips = StringUtils.split(value, ",");
            if (ips.length > 0) {
                return StringUtils.trim(ips[0]);
            }
        }
        return defaultValue;
    }

    protected String parseClientIp(ApiRequest apiRequest, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        clientIp = getOverrideHeader(request, overrideClientIpHeader, clientIp);
        clientIp = getOverrideHeader(request, FORWARDED_FOR_HEADER, clientIp, false);

        return clientIp;
    }

    /**
     * Constructs the request URL based off of standard headers in the request, falling back to the HttpServletRequest.getRequestURL()
     * if the headers aren't available. Here is the ordered list of how we'll attempt to construct the URL:
     *  - x-api-request-url
     *  - x-forwarded-proto://x-forwarded-host:x-forwarded-port/HttpServletRequest.getRequestURI()
     *  - x-forwarded-proto://x-forwarded-host/HttpServletRequest.getRequestURI()
     *  - x-forwarded-proto://host:x-forwarded-port/HttpServletRequest.getRequestURI()
     *  - x-forwarded-proto://host/HttpServletRequest.getRequestURI() request.getRequestURL()
     *
     * Additional notes:
     *  - With x-api-request-url, the query string is passed, it will be dropped to match the other formats.
     *  - If the x-forwarded-host/host header has a port and x-forwarded-port has been passed, x-forwarded-port will be used.
     */
    protected String parseRequestUrl(ApiRequest apiRequest, HttpServletRequest request) {
        // Get url from custom x-api-request-url header
        String requestUrl = getOverrideHeader(request, overrideUrlHeader, null);
        if (requestUrl != null) {
            String[] parts = requestUrl.split("\\?", 2);
            return parts[0];
        }

        // Get url from standard headers
        requestUrl = getUrlFromStandardHeaders(request);
        if (requestUrl != null) {
            return requestUrl;
        }

        // Use incoming url
        return request.getRequestURL().toString();
    }

    private String getUrlFromStandardHeaders(HttpServletRequest request) {
        String host = getOverrideHeader(request, FORWARDED_HOST_HEADER, null, false);
        if (host == null) {
            host = getOverrideHeader(request, HOST_HEADER, null, false);
        }

        String port = getOverrideHeader(request, FORWARDED_PORT_HEADER, null, false);
        String xForwardedProto = getOverrideHeader(request, FORWARDED_PROTO_HEADER, null, false);

        if (xForwardedProto == null && isHttpsPort(host, port)) {
            xForwardedProto = "https";
        }

        if (xForwardedProto == null || host == null) {
            return null;
        }

        if (StringUtils.equals(port, "443") || StringUtils.equals(port, "80")) {
            port = null; // Don't include default ports in url
        }

        if (port != null && host.contains(":")) {
            // Have to strip the port that is in the host. Handle IPv6, which has this format: [::1]:8080
            if ((host.startsWith("[") && host.contains("]:")) || !host.startsWith("[")) {
                host = host.substring(0, host.lastIndexOf(":"));
            }
        }

        StringBuilder builder = new StringBuilder(xForwardedProto).append("://").append(host);
        if (port != null) {
            builder.append(":").append(port);
        }
        builder.append(request.getRequestURI());

        return builder.toString();
    }

    protected String parseResponseUrlBase(ApiRequest apiRequest, HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String requestUrl = apiRequest.getRequestUrl();

        int index = requestUrl.lastIndexOf(servletPath);
        if (index == -1) {
            try {
                /*
                 * Fallback, if we can't find servletPath in requestUrl, then we just assume the base is the root of the web request
                 */
                URL url = new URL(requestUrl);
                StringBuilder buffer = new StringBuilder(url.getProtocol()).append("://").append(url.getHost());

                if (url.getPort() != -1) {
                    buffer.append(":").append(url.getPort());
                }
                return buffer.toString();
            } catch (MalformedURLException e) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
        } else {
            return requestUrl.substring(0, index);
        }
    }

    protected String parseVersion(ApiRequest apiRequest, HttpServletRequest request) {
        return parseVersion(request.getServletPath());
    }

    @Override
    public String parseVersion(String servletPath) {
        servletPath = trimPrefix(servletPath.replaceAll("//+", "/"));

        if (!servletPath.startsWith("/") || servletPath.length() < 2)
            return null;

        return servletPath.split("/")[1];
    }

    protected String trimPrefix(String path) {
        if (trimPrefix != null && path.startsWith(trimPrefix)) {
            return path.substring(trimPrefix.length());
        }

        return path;
    }

    protected String parseResponseType(ApiRequest apiRequest, HttpServletRequest request) {
        String format = request.getParameter("_format");

        if (format != null) {
            format = format.toLowerCase().trim();
        }

        /* Format specified */
        if (format != null && allowedFormats.contains(format)) {
            return format;
        }

        // User agent has Mozilla and browser accepts */*
        if (RequestUtils.isBrowser(request, true)) {
            return HTML;
        } else {
            return JSON;
        }
    }

    protected void parsePath(ApiRequest apiRequest, HttpServletRequest request) {
        if (apiRequest.getVersion() == null)
            return;

        String servletPath = request.getServletPath();
        servletPath = trimPrefix(servletPath.replaceAll("//+", "/"));

        String versionPrefix = "/" + apiRequest.getVersion();
        if (!servletPath.startsWith(versionPrefix)) {
            return;
        }

        String[] parts = servletPath.substring(versionPrefix.length()).split("/");

        String typeName = indexValue(parts, 1);
        String id = indexValue(parts, 2);
        String link = indexValue(parts, 3);

        if (StringUtils.isBlank(typeName)) {
            return;
        } else {
            SchemaFactory schemaFactory = apiRequest.getSchemaFactory();
            if (schemaFactory == null) {
                apiRequest.setType(typeName);
            } else {
                String singleType = apiRequest.getSchemaFactory().getSingularName(typeName);
                apiRequest.setType(singleType == null ? typeName : singleType);
            }
        }

        if (StringUtils.isBlank(id)) {
            return;
        } else {
            apiRequest.setId(id);
        }

        if (StringUtils.isBlank(link)) {
            return;
        } else {
            apiRequest.setLink(link);
        }
    }

    protected String indexValue(String[] array, int index) {
        if (array.length <= index) {
            return null;
        }
        String value = array[index];
        return value == null ? value : value.trim();
    }

    @PostConstruct
    public void init() {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(maxUploadSize * 2);

        servletFileUpload = new ServletFileUpload(factory);
        servletFileUpload.setFileSizeMax(maxUploadSize);
        servletFileUpload.setSizeMax(maxUploadSize);

        if (allowedFormats == null) {
            allowedFormats = new HashSet<String>();
            allowedFormats.add(HTML);
            allowedFormats.add(JSON);
        }
    }

    public boolean isHttpsPort(String host, String port) {
        return false;
    }

    public Set<String> getAllowedFormats() {
        return allowedFormats;
    }

    public void setAllowedFormats(Set<String> allowedFormats) {
        this.allowedFormats = allowedFormats;
    }

    public boolean isAllowClientOverrideHeaders() {
        return allowClientOverrideHeaders;
    }

    public void setAllowClientOverrideHeaders(boolean allowClientOverrideHeaders) {
        this.allowClientOverrideHeaders = allowClientOverrideHeaders;
    }

    public String getTrimPrefix() {
        return trimPrefix;
    }

    public void setTrimPrefix(String trimPrefix) {
        this.trimPrefix = trimPrefix;
    }

}