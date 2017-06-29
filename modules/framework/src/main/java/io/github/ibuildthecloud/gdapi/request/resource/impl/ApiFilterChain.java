package io.github.ibuildthecloud.gdapi.request.resource.impl;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ValidationFilter;

import java.util.List;
import java.util.Map;

public class ApiFilterChain implements ResourceManager, ActionHandler {

    ValidationFilter filter;
    ResourceManager next;
    ActionHandler nextActionHandler;

    public ApiFilterChain(ValidationFilter filter, ResourceManager next) {
        super();
        this.filter = filter;
        this.next = next;
    }

    public ApiFilterChain(ValidationFilter filter, ActionHandler next) {
        super();
        this.filter = filter;
        this.nextActionHandler = next;
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return filter.getById(type, id, options, next);
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
    public Object perform(String name, Object obj, ApiRequest request) {
        return filter.perform(name, obj, request, nextActionHandler);
    }

}
