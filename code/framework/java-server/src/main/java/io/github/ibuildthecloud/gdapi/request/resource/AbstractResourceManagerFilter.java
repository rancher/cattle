package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

public abstract class AbstractResourceManagerFilter implements ResourceManagerFilter {

    @Override
    public String[] getTypes() {
        return new String[] {};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

    @Override
    public Object getById(String type, String id, ListOptions options, ResourceManager next) {
        return next.getById(type, id, options);
    }

    @Override
    public Object getLink(String type, String id, String link, ApiRequest request, ResourceManager next) {
        return next.getLink(type, id, link, request);
    }

    @Override
    public Object list(String type, ApiRequest request, ResourceManager next) {
        return next.list(type, request);
    }

    @Override
    public List<?> list(String type, Map<Object, Object> criteria, ListOptions options, ResourceManager next) {
        return next.list(type, criteria, options);
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        return next.create(type, request);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        return next.update(type, id, request);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        return next.delete(type, id, request);
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        return next.resourceAction(type, request);
    }

    @Override
    public Object collectionAction(String type, ApiRequest request, ResourceManager next) {
        return next.collectionAction(type, request);
    }

}
