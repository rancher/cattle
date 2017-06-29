package io.cattle.platform.iaas.api.filter.settings;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.List;
import java.util.Map;

public class AuthorizationResourceManagerWrapper implements ResourceManager {

    ResourceManager resourceManager;
    ResourceManagerAuthorizer authorizer;

    public AuthorizationResourceManagerWrapper(ResourceManager resourceManager, ResourceManagerAuthorizer authorizer) {
        this.resourceManager = resourceManager;
        this.authorizer = authorizer;
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return authorizer.authorize(resourceManager.getById(type, id, options));
    }

    @Override
    public Object list(String type, ApiRequest request) {
        return authorizer.authorize(resourceManager.list(type, request));
    }

    @Override
    public List<?> list(String type, Map<Object, Object> criteria, ListOptions options) {
        return (List<?>) authorizer.authorize(resourceManager.list(type, criteria, options));
    }

    @Override
    public Object create(String type, ApiRequest request) {
        return authorizer.authorize(resourceManager.create(type, request));
    }

    @Override
    public Object update(String type, String id, ApiRequest request) {
        return authorizer.authorize(resourceManager.update(type, id, request));
    }

    @Override
    public Object delete(String type, String id, ApiRequest request) {
        return authorizer.authorize(resourceManager.delete(type, id, request));
    }

}