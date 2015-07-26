package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

public class ResponseObjectConverter extends AbstractApiRequestHandler {

    ResourceManagerLocator resourceManagerLocator;

    @Override
    public void handle(ApiRequest request) throws IOException {
        Object response = request.getResponseObject();
        if (response == null)
            return;

        if (response instanceof Resource || response instanceof Collection || response instanceof InputStream) {
            return;
        }

        ResourceManager resourceManager = resourceManagerLocator.getResourceManager(request);
        if (resourceManager == null)
            return;

        if (response instanceof List) {
            response = resourceManager.convertResponse((List<?>)response, request);
        } else {
            response = resourceManager.convertResponse(response, request);
        }

        request.setResponseObject(response);
    }

    public ResourceManagerLocator getResourceManagerLocator() {
        return resourceManagerLocator;
    }

    @Inject
    public void setResourceManagerLocator(ResourceManagerLocator resourceManagerLocator) {
        this.resourceManagerLocator = resourceManagerLocator;
    }

}
