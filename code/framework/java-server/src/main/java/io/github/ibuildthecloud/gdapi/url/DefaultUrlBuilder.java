package io.github.ibuildthecloud.gdapi.url;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Sort.SortOrder;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public final class DefaultUrlBuilder implements UrlBuilder {

    private static final String REMOVE_PARAM_REGEXP = "&?%s=[^&]*";

    ApiRequest apiRequest;
    SchemaFactory schemaFactory;
    String subContext = "";

    public DefaultUrlBuilder(ApiRequest apiRequest, SchemaFactory schemaFactory) {
        this.apiRequest = apiRequest;
        this.schemaFactory = schemaFactory;
    }

    @Override
    public URL resourceReferenceLink(Resource resource) {
        return constructBasicUrl(getPluralName(resource), resource.getId());
    }

    @Override
    public URL resourceReferenceLink(Class<?> type, String id) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();

        Schema schema = schemaFactory.getSchema(type);
        return schema == null ? null : constructBasicUrl(schema.getPluralName(), formatter.formatId(schema.getId(), id).toString());
    }

    @Override
    public URL resourceReferenceLink(String type, String id) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();

        Schema schema = schemaFactory.getSchema(type);
        if (schema == null) {
            return constructBasicUrl(false, type, formatter.formatId(type, id).toString());
        } else {
            return constructBasicUrl(schema.getPluralName(), formatter.formatId(schema.getId(), id).toString());
        }
    }

    protected String getPluralName(Resource resource) {
        return getPluralName(resource.getType());
    }

    protected String getPluralName(String type) {
        return schemaFactory.getPluralName(type);
    }

    protected URL constructBasicUrl(boolean lowercase, String... parts) {
        StringBuilder builder = new StringBuilder().append(apiRequest.getResponseUrlBase()).append("/").append(apiRequest.getVersion()).append(subContext);

        for (String part : parts) {
            if (part == null)
                return null;
            builder.append("/").append(part);
        }

        if (lowercase) {
            return toURL(builder.toString().toLowerCase());
        } else {
            return toURL(builder.toString());
        }
    }

    protected URL constructBasicUrl(String... parts) {
        return constructBasicUrl(true, parts);
    }

    @Override
    public URL resourceCollection(Class<?> type) {
        Schema schema = schemaFactory.getSchema(type);
        return schema == null ? null : constructBasicUrl(schema.getPluralName());
    }

    @Override
    public URL resourceCollection(String type) {
        String plural = getPluralName(type);
        return plural == null ? null : constructBasicUrl(getPluralName(type));
    }

    @Override
    public URL resourceLink(Class<?> type, String id, String name) {
        if (name == null)
            return null;

        IdFormatter formatter = ApiContext.getContext().getIdFormatter();

        String typeName = schemaFactory.getSchemaName(type);
        String pluralName = schemaFactory.getPluralName(typeName);

        return constructBasicUrl(pluralName, id == null ? null : formatter.formatId(typeName, id).toString(), name.toLowerCase());
    }

    @Override
    public URL resourceLink(Resource resource, String name) {
        if (name == null)
            return null;

        return constructBasicUrl(getPluralName(resource), resource.getId(), name.toLowerCase());
    }

    @Override
    public URL reverseSort(SortOrder currentOrder) {
        StringBuilder buffer = fullUrlToAppendQueryString(Collection.ORDER, Collection.MARKER);

        buffer.append(Collection.ORDER).append("=").append(currentOrder.getReverseExternalForm());

        return toURL(buffer.toString());
    }

    @Override
    public URL actionLink(Resource resource, String name) {
        return constructBasicUrl(getPluralName(resource), resource.getId(), "?" + Resource.ACTION + "=" + name);
    }

    @Override
    public URL sort(String field) {
        StringBuilder buffer = fullUrlToAppendQueryString(Collection.SORT, Collection.ORDER);

        buffer.append(Collection.SORT).append("=").append(field);

        return toURL(buffer.toString());
    }

    @Override
    public URL next(String id) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();

        StringBuilder buffer = fullUrlToAppendQueryString(Collection.MARKER);

        Object formatted = id;
        if (id != null && id.length() > 0 && id.charAt(0) != 'm') {
            formatted = formatter.formatId(Collection.MARKER, id);
        }

        buffer.append(Collection.MARKER).append("=").append(formatted);

        return toURL(buffer.toString());
    }

    protected StringBuilder fullUrlToAppendQueryString(String... removes) {
        StringBuilder buffer = new StringBuilder(apiRequest.getRequestUrl());
        buffer.append("?");

        String queryString = removeParameter(apiRequest.getQueryString(), removes);
        buffer.append(queryString);

        if (queryString.length() > 0) {
            buffer.append("&");
        }

        return buffer;
    }

    protected URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to create URL for [" + url + "]", e);
        }
    }

    protected String removeParameter(String queryString, String... names) {
        if (queryString == null)
            return "";

        for (String name : names) {
            String pattern = String.format(REMOVE_PARAM_REGEXP, Pattern.quote(name));
            queryString = queryString.replaceAll(pattern, "");
        }

        return queryString;
    }

    @Override
    public URL version(String version) {
        StringBuilder builder = new StringBuilder().append(apiRequest.getResponseUrlBase()).append("/").append(version);

        return toURL(builder.toString());
    }

    @Override
    public URL current() {
        return toURL(apiRequest.getRequestUrl());
    }

    @Override
    public URL staticResource(String... parts) {
        StringBuilder builder = new StringBuilder().append(apiRequest.getResponseUrlBase()).append("/").append(apiRequest.getStaticResourceBase());

        for (String part : parts) {
            if (part == null)
                return null;
            builder.append("/").append(part);
        }

        return toURL(builder.toString().toLowerCase());
    }

    public String getSubContext() {
        return subContext;
    }

    public void setSubContext(String subContext) {
        this.subContext = subContext;
    }

}
