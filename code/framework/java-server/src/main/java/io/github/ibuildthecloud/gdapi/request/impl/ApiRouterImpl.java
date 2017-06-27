package io.github.ibuildthecloud.gdapi.request.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRouter;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.request.resource.ValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.impl.ApiFilterChain;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.response.impl.ResourceOutputFilterChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiRouterImpl implements ResourceManagerLocator, ApiRouter {

    Map<Object, ResourceManager> cached = new ConcurrentHashMap<>();
    Map<Object, ActionHandler> cachedAction = new ConcurrentHashMap<>();

    List<ApiRequestHandler> handlers = new ArrayList<>();
    Map<String, ResourceManager> rmByType = new HashMap<>();
    Map<String, List<ValidationFilter>> filterByType = new HashMap<>();
    Map<String, List<LinkHandler>> linkByType = new HashMap<>();
    Map<String, ActionHandler> actionHandlersMap = new HashMap<>();
    Map<String, List<ResourceOutputFilter>> outputFiltersByType = new HashMap<>();
    Map<String, ResourceOutputFilter> outputFilterByType = new ConcurrentHashMap<>();
    ResourceManager defaultResourceManager;
    ActionHandler defaultActionHandler;
    SchemaFactory schemaFactory;

    public ApiRouterImpl(SchemaFactory schemaFactory) {
        super();
        this.schemaFactory = schemaFactory;
    }

    @Override
    public ApiRouterImpl handler(ApiRequestHandler handler) {
        handlers.add(handler);
        return this;
    }

    @Override
    public List<ApiRequestHandler> getHandlers() {
        return handlers;
    }

    @Override
    public ApiRouterImpl handlers(ApiRequestHandler... handlers) {
        for (ApiRequestHandler handler : handlers) {
            handler(handler);
        }
        return this;
    }

    @Override
    public ApiRouterImpl resourceManager(Class<?> type, ResourceManager rm) {
        for (String t : schemaFactory.getSchemaNames(type)) {
            resourceManager(t, rm);
        }
        return this;
    }

    @Override
    public ApiRouterImpl resourceManager(String type, ResourceManager rm) {
        rmByType.put(type, rm);
        return this;
    }

    @Override
    public ApiRouterImpl filter(Class<?> type, ValidationFilter filter) {
        for (String t : schemaFactory.getSchemaNames(type)) {
            filter(t, filter);
        }
        return this;
    }

    @Override
    public ApiRouterImpl filter(String type, ValidationFilter filter) {
        List<ValidationFilter> filters = filterByType.get(type);
        if (filters == null) {
            filters = new ArrayList<>();
            filterByType.put(type, filters);
        }
        filters.add(filter);
        return this;
    }

    @Override
    public ApiRouterImpl outputFilter(Class<?> type, ResourceOutputFilter filter) {
        for (String t : schemaFactory.getSchemaNames(type)) {
            outputFilter(t, filter);
        }
        return this;
    }

    @Override
    public ApiRouterImpl outputFilter(String type, ResourceOutputFilter filter) {
        List<ResourceOutputFilter> handlers = outputFiltersByType.get(type);
        if (handlers == null) {
            handlers = new ArrayList<>();
            outputFiltersByType.put(type, handlers);
        }
        handlers.add(filter);
        return this;
    }

    @Override
    public ApiRouterImpl link(Class<?> type, LinkHandler link) {
        for (String t : schemaFactory.getSchemaNames(type)) {
            link(t, link);
        }
        return this;
    }

    @Override
    public ApiRouterImpl link(String type, LinkHandler link) {
        List<LinkHandler> handlers = linkByType.get(type);
        if (handlers == null) {
            handlers = new ArrayList<>();
            linkByType.put(type, handlers);
        }
        handlers.add(link);
        return this;
    }

    @Override
    public ApiRouterImpl action(String name, ActionHandler action) {
        actionHandlersMap.put(name, action);
        return this;
    }

    @Override
    public ApiRouterImpl defaultResourceManager(ResourceManager rm) {
        defaultResourceManager = rm;
        return this;
    }

    @Override
    public ApiRouterImpl defaultActionHandler(ActionHandler action) {
        defaultActionHandler = action;
        return this;
    }

    @Override
    public ResourceManager getResourceManagerByType(String type) {
        if (type == null) {
            return null;
        }

        ResourceManager rm = cached.get(type);
        if (rm != null) {
            return rm;
        }

        rm = rmByType.get(type);

        if (rm == null) {
            rm = defaultResourceManager;
        }

        if (rm == null) {
            return rm;
        }

        List<ValidationFilter> filters = filterByType.get(type);
        if (filters == null) {
            return rm;
        }

        rm = wrap(filters, rm);
        cached.put(type, rm);

        return rm;
    }

    protected ResourceManager wrap(List<ValidationFilter> filters, ResourceManager resourceManager) {
        if (filters.size() == 0) {
            return resourceManager;
        }

        if (filters.size() == 1) {
            return new ApiFilterChain(filters.get(0), resourceManager);
        }

        return new ApiFilterChain(filters.get(0), wrap(filters.subList(1, filters.size()), resourceManager));
    }

    protected ActionHandler wrapAction(List<ValidationFilter> filters, ActionHandler actionHandler) {
        if (filters.size() == 0) {
            return actionHandler;
        }

        if (filters.size() == 1) {
            return new ApiFilterChain(filters.get(0), actionHandler);
        }

        return new ApiFilterChain(filters.get(0), wrapAction(filters.subList(1, filters.size()), actionHandler));
    }

    protected void add(Map<String, List<ValidationFilter>> filters, String key, ValidationFilter filter) {
        List<ValidationFilter> list = filters.get(key);
        if (list == null) {
            list = new ArrayList<>();
            filters.put(key, list);
        }

        list.add(filter);
    }

    @Override
    public List<LinkHandler> getLinkHandlersByType(String type) {
        return linkByType.get(type);
    }

    @Override
    public ActionHandler getActionHandler(String name, String type) {
        if (name == null) {
            return null;
        }

        ActionHandler rm = cachedAction.get(name);
        if (rm != null) {
            return rm;
        }

        rm = actionHandlersMap.get(name);

        if (rm == null) {
            rm = defaultActionHandler;
        }

        if (rm == null) {
            return rm;
        }

        List<ValidationFilter> filters = filterByType.get(type);
        if (filters == null) {
            return rm;
        }

        rm = wrapAction(filters, rm);
        cachedAction.put(name, rm);

        return rm;
    }

    @Override
    public ResourceOutputFilter getOutputFilter(Resource resource) {
        if (resource == null) {
            return null;
        }

        String type = resource.getType();

        ResourceOutputFilter outputFilter = outputFilterByType.get(type);
        if (outputFilter != null) {
            return outputFilter;
        }

        if (outputFilter == null && !outputFiltersByType.containsKey(type)) {
            return null;
        }

        outputFilter = buildChain(type);
        if (outputFilter != null) {
            outputFilterByType.put(type, buildChain(type));
        }

        return outputFilter;
    }

    protected ResourceOutputFilter buildChain(String type) {
        List<ResourceOutputFilter> outputFilters = outputFiltersByType.get(type);
        if (outputFilters == null) {
            return null;
        }

        ResourceOutputFilter result = null;

        for (ResourceOutputFilter filter : outputFilters) {
            if (result == null) {
                result = filter;
            } else {
                result = new ResourceOutputFilterChain(result, filter);
            }
        }

        return result;
    }

}
