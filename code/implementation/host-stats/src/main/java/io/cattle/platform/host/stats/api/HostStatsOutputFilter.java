package io.cattle.platform.host.stats.api;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.host.stats.utils.HostStatsConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class HostStatsOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        boolean add = false;

        if (original instanceof Host && DockerHostConstants.KIND_DOCKER.equals(((Host) original).getKind())) {
            add = true;
        } else if (original instanceof Instance && InstanceConstants.KIND_CONTAINER.equals(((Instance) original).getKind())) {
            add = true;
        }

        if (add) {
            converted.getLinks().put(HostStatsConstants.LINK_STATS, ApiContext.getUrlBuilder().resourceLink(converted, HostStatsConstants.LINK_STATS));
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE_CONTAINER, HostConstants.TYPE };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

}