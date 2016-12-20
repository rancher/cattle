package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.ActionLinksMapper;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class JsonResponseWriter extends AbstractApiRequestHandler {

    JsonMapper jsonMapper;
    JsonMapper actionLinksMapper = new ActionLinksMapper();
    boolean chunked = true;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (request.isCommitted())
            return;

        if (!getResponseFormat().equals(request.getResponseFormat())) {
            return;
        }

        Object responseObject = getResponseObject(request);

        if (responseObject == null)
            return;

        request.setResponseContentType(getContentType());

        JsonMapper jsonMapper = this.jsonMapper;
        if (request.getServletContext().getRequest().getHeader("X-API-Action-Links") != null) {
            jsonMapper = actionLinksMapper;
        }

        OutputStream os = request.getOutputStream();
        BufferedOutputStream buf = new BufferedOutputStream(os);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeJson(jsonMapper, chunked ? buf : baos, responseObject, request);
        buf.flush();

        if (!chunked) {
            byte[] bytes = baos.toByteArray();
            request.getServletContext().getResponse().setContentLength(bytes.length);
            os.write(bytes);
            os.flush();
        }
    }

    protected String getContentType() {
        return "application/json; charset=utf-8";
    }

    protected String getResponseFormat() {
        return "json";
    }

    protected Object getResponseObject(ApiRequest request) {
        Object object = request.getResponseObject();

        if (object instanceof List) {
            return createCollection((List<?>)object, request);
        } else if (object instanceof Collection) {
            return object;
        }

        return createResource(request.getSchemaFactory(), object);
    }

    protected Collection createCollection(List<?> list, ApiRequest request) {
        CollectionImpl collection = new CollectionImpl();
        collection.setResourceType(request.getType());

        for (Object obj : list) {
            Resource resource = createResource(request.getSchemaFactory(), obj);
            if (resource != null) {
                collection.getData().add(resource);
                if (collection.getResourceType() == null) {
                    collection.setResourceType(resource.getType());
                }
            }
        }

        return collection;
    }

    protected Resource createResource(SchemaFactory schemaFactory, Object obj) {
        if (obj == null)
            return null;

        if (obj instanceof Resource)
            return (Resource)obj;

        Schema schema = schemaFactory.getSchema(obj.getClass());
        ApiContext apiContext = ApiContext.getContext();
        return schema == null ? null : new WrappedResource(apiContext.getIdFormatter(), schemaFactory, schema, obj, apiContext.getApiRequest().getMethod());
    }

    protected void writeJson(JsonMapper jsonMapper, OutputStream os, Object responseObject, ApiRequest request) throws IOException {
        jsonMapper.writeValue(os, responseObject);
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

}
