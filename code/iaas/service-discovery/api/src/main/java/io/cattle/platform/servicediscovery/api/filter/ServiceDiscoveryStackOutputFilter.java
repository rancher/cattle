package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class ServiceDiscoveryStackOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Stack) {
            converted.getLinks().put(ServiceConstants.LINK_COMPOSE_CONFIG,
                    ApiContext.getUrlBuilder().resourceLink(converted,
                            ServiceConstants.LINK_COMPOSE_CONFIG));
        }

        return converted;
    }

}
