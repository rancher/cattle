package io.github.ibuildthecloud.gdapi.request;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.server.model.ApiServletContext;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class ApiRequest {

    Locale locale;
    String type;
    String id;
    String link;
    String action;
    String method;
    String clientIp;
    String queryString;
    String staticResourceBase = "static";
    boolean committed = false;
    int responseCode = HttpServletResponse.SC_OK;
    ApiServletContext apiServletContext;
    Object responseObject;
    Object requestObject;
    Map<String, Object> requestParams;
    String requestUrl;
    String version;
    String schemaVersion;
    String responseUrlBase;
    String responseFormat;
    String responseContentType;
    String subContext = "";

    long startTime = System.currentTimeMillis();
    UrlBuilder urlBuilder;
    Map<String, List<Condition>> conditions = new LinkedHashMap<String, List<Condition>>();
    Sort sort;
    Pagination pagination;
    Include include;
    Map<Object, Object> attributes = new HashMap<Object, Object>();
    Map<String, String> options = new HashMap<String, String>();
    SchemaFactory schemaFactory;
    Map<String, Object> createDefaults = new HashMap<String, Object>();
    List<Throwable> exceptions = new ArrayList<Throwable>();

    public ApiRequest(ApiRequest request) {
        this.locale = request.getLocale();
        this.type = request.getType();
        this.id = request.getId();
        this.link = request.getLink();
        this.action = request.getAction();
        this.method = request.getMethod();
        this.clientIp = request.getClientIp();
        this.queryString = request.getQueryString();
        this.committed = request.isCommitted();
        this.responseCode = request.getResponseCode();
        this.requestObject = request.getRequestObject();
        this.requestParams = request.getRequestParams();
        this.requestUrl = request.getRequestUrl();
        this.version = request.getVersion();
        this.schemaVersion = request.getSchemaVersion();
        this.responseUrlBase = request.getResponseUrlBase();
        this.responseFormat = request.getResponseFormat();
        this.responseContentType = request.getResponseContentType();
        this.startTime = request.getStartTime();
        this.urlBuilder = request.getUrlBuilder();
        this.conditions = request.getConditions();
        this.sort = request.getSort();
        this.pagination = request.getPagination();
        this.include = request.getInclude();
        this.schemaFactory = request.getSchemaFactory();
        this.createDefaults = request.getCreateDefaults();
        this.exceptions = request.getExceptions();
        this.options = request.getOptions();
        this.staticResourceBase = request.getStaticResourceBase();
        this.subContext = request.getSubContext();
    }

    public ApiRequest(ApiServletContext requestServletContext, SchemaFactory schemaFactory) {
        super();
        this.apiServletContext = requestServletContext;
        this.locale = requestServletContext == null ? null : requestServletContext.getRequest().getLocale();
        this.schemaFactory = schemaFactory;
    }

    public ApiRequest reset() {
        type = null;
        id = null;
        link = null;
        action = null;
        method = null;
        queryString = null;
        committed = false;
        responseCode = HttpServletResponse.SC_OK;
        responseObject = null;
        requestObject = null;
        requestParams = null;
        version = null;
        schemaVersion = null;
        startTime = System.currentTimeMillis();
        conditions = new LinkedHashMap<String, List<Condition>>();
        sort = null;
        pagination = null;
        include = null;
        attributes = new HashMap<Object, Object>();
        options = new HashMap<String, String>();
        createDefaults = new HashMap<String, Object>();
        exceptions = new ArrayList<Throwable>();
        subContext = "";

        return this;
    }

    public InputStream getInputStream() throws IOException {
        if (apiServletContext == null) {
            return null;
        }
        return apiServletContext.getRequest().getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        if (committed) {
            throw new IllegalStateException("Response is commited");
        }
        commit();
        committed = true;
        return apiServletContext.getResponse().getOutputStream();
    }

    public <T> T proxyRequestObject(Class<T> type) {
        Map<String, Object> map = RequestUtils.toMap(requestObject);
        return ProxyUtils.proxy(map, type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isCommitted() {
        return committed;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Object getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(Object responseObject) {
        this.responseObject = responseObject;
    }

    public Object getRequestObject() {
        return requestObject;
    }

    public void setRequestObject(Object requestObject) {
        this.requestObject = requestObject;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(Map<String, Object> requestParams) {
        this.requestParams = requestParams;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getResponseUrlBase() {
        return responseUrlBase;
    }

    public void setResponseUrlBase(String responseUrlBase) {
        this.responseUrlBase = responseUrlBase;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseType) {
        this.responseFormat = responseType;
    }

    public void setResponseContentType(String contentType) {
        this.responseContentType = contentType;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public ApiServletContext getServletContext() {
        return apiServletContext;
    }

    public void setRequestServletContext(ApiServletContext requestServletContext) {
        this.apiServletContext = requestServletContext;
    }

    public long getStartTime() {
        return startTime;
    }

    public void commit() {
        if (!committed) {
            if (responseContentType != null) {
                apiServletContext.getResponse().setHeader("Content-Type", responseContentType);
            }
            apiServletContext.getResponse().setStatus(responseCode);

            committed = true;
        }
    }

    public UrlBuilder getUrlBuilder() {
        return urlBuilder;
    }

    public void setUrlBuilder(UrlBuilder urlWriter) {
        this.urlBuilder = urlWriter;
    }

    public Map<String, List<Condition>> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, List<Condition>> conditions) {
        this.conditions = conditions;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Include getInclude() {
        return include;
    }

    public void setInclude(Include include) {
        this.include = include;
    }

    public Object getAttribute(Object object) {
        return this.attributes.get(object);
    }

    public void setAttribute(Object key, Object value) {
        this.attributes.put(key, value);
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
        this.urlBuilder = null;
    }

    public Map<String, Object> getCreateDefaults() {
        return createDefaults;
    }

    public void setCreateDefaults(Map<String, Object> createDefaults) {
        this.createDefaults = createDefaults;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getStaticResourceBase() {
        return staticResourceBase;
    }

    public void setStaticResourceBase(String staticResourceBase) {
        this.staticResourceBase = staticResourceBase;
    }

    public String getSubContext() {
        return subContext;
    }

    public void setSubContext(String subContext) {
        this.subContext = subContext;
    }

    @Override
    public String toString() {
        return requestUrl == null ? "" : requestUrl;
    }

    public String getSchemaVersion() {
        if (schemaVersion == null) {
            return version;
        }
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}