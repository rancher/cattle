package io.github.ibuildthecloud.gdapi.request.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRouter;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.request.resource.ValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.impl.ApiFilterChain;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ApiRouterImpl implements ResourceManagerLocator, ApiRouter {

    Map<Object, ResourceManager> cached = new ConcurrentHashMap<>();
    Map<Object, ActionHandler> cachedAction = new ConcurrentHashMap<>();

    List<ApiRequestHandler> handlers = new ArrayList<>();
    Map<String, ResourceManager> rmByType = new HashMap<>();
    ListValuedMap<String, ValidationFilter> filterByType = new ArrayListValuedHashMap<>();
    ListValuedMap<String, LinkHandler> linkByType = new ArrayListValuedHashMap<>();
    Map<String, ActionHandler> actionHandlersMap = new HashMap<>();
    ListValuedMap<String, ResourceOutputFilter> outputFiltersByType = new ArrayListValuedHashMap<>();
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
    public ApiRouterImpl resourceManager(Object type, ResourceManager rm) {
        forEach(type, (typeName) -> rmByType.put(typeName, rm));
        return this;
    }

    private Schema getSchema(Object obj) {
        if (obj instanceof Class<?>) {
            return schemaFactory.getSchema((Class<?>) obj);
        } else if (obj instanceof String) {
            return schemaFactory.getSchema((String) obj);
        }
        return null;
    }

    private void forEach(Object obj, Consumer<String> consumer) {
        Schema schema = getSchema(obj);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown schema type [" + obj + "]");
        }

        consumer.accept(schema.getId());
        for (String child : schema.getChildren()) {
            forEach(child, consumer);
        }
    }

    @Override
    public ApiRouterImpl validationFilter(Object type, ValidationFilter filter) {
        forEach(type, typeName -> filterByType.put(typeName, filter));
        return this;
    }

    @Override
    public ApiRouterImpl outputFilter(Object type, ResourceOutputFilter filter) {
        forEach(type, typeName -> outputFiltersByType.put(typeName, filter));
        return this;
    }

    @Override
    public ApiRouterImpl link(Object type, LinkHandler link) {
        forEach(type, typeName -> linkByType.put(typeName, link));
        return this;
    }

    @Override
    public ApiRouterImpl action(Object type, String name, ActionHandler action) {
        forEach(type, typeName -> {
            Schema schema = schemaFactory.getSchema(typeName);
            if (schema.getResourceActions().containsKey(name)) {
                actionHandlersMap.put(String.format("%s.%s", typeName, name).toLowerCase(), action);
            } else {
                // A subtype could drop the action in authorization, so don't blow up if the
                // action is defined in the original type requested
                schema = getSchema(type);
                if (!schema.getResourceActions().containsKey(type)) {
                    throw new IllegalArgumentException("Invalid action [" + name + "] on type [" + typeName + "] " +
                        "known actions " + schema.getResourceActions().keySet());
                }
            }
        });
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
            return null;
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
    public List<ResourceOutputFilter> getOutputFilters(Resource resource) {
        if (resource == null) {
            return null;
        }

        String type = resource.getType();
        return outputFiltersByType.get(type);
    }

}
