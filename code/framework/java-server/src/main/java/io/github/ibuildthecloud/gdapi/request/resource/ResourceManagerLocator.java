package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.List;

public interface ResourceManagerLocator {

    default ResourceManager getResourceManager(ApiRequest request) {
        if (request.getType() == null)
            return null;

        return getResourceManagerByType(request.getType());
    }

    ResourceManager getResourceManagerByType(String type);

    List<LinkHandler> getLinkHandlersByType(String type);

    ActionHandler getActionHandler(String name, String type);

    ResourceOutputFilter getOutputFilter(Resource resource);

}
