package io.github.ibuildthecloud.gdapi.request.resource.impl;

import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerFilter;

import java.util.List;
import java.util.Map;

public class FilteredResourceManager implements ResourceManager {

    ResourceManagerFilter filter;
    ResourceManager next;

    public FilteredResourceManager(ResourceManagerFilter filter, ResourceManager next) {
        super();
        this.filter = filter;
        this.next = next;
    }

    @Override
    public String[] getTypes() {
        return next.getTypes();
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return next.getTypeClasses();
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return filter.getById(type, id, options, next);
    }

    @Override
    public Object getLink(String type, String id, String link, ApiRequest request) {
        return filter.getLink(type, id, link, request, next);
    }

    @Override
    public Object list(String type, ApiRequest request) {
        return filter.list(type, request, next);
    }

    @Override
    public List<?> list(String type, Map<Object, Object> criteria, ListOptions options) {
        return filter.list(type, criteria, options, next);
    }

    @Override
    public Object create(String type, ApiRequest request) {
        return filter.create(type, request, next);
    }

    @Override
    public Object update(String type, String id, ApiRequest request) {
        return filter.update(type, id, request, next);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request) {
        return filter.delete(type, id, request, next);
    }

    @Override
    public Collection convertResponse(List<?> object, ApiRequest request) {
        return next.convertResponse(object, request);
    }

    @Override
    public Resource convertResponse(Object obj, ApiRequest request) {
        return next.convertResponse(obj, request);
    }

    @Override
    public boolean handleException(Throwable t, ApiRequest request) {
        return next.handleException(t, request);
    }

    @Override
    public Object resourceAction(String type, ApiRequest request) {
        return filter.resourceAction(type, request, next);
    }

    @Override
    public Object collectionAction(String type, ApiRequest request) {
        return filter.collectionAction(type, request, next);
    }

}
