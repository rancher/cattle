package io.cattle.platform.extension.api.manager;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class ResourceDefinitionOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        converted.getLinks().put("processes", null);
        converted.getLinks().put("resourceDot", null);
        return converted;
    }

}
