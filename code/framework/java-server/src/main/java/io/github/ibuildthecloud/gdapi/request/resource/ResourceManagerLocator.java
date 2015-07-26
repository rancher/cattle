package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ResourceManagerLocator {

    ResourceManager getResourceManager(ApiRequest request);

    ResourceManager getResourceManagerByType(String type);

}
