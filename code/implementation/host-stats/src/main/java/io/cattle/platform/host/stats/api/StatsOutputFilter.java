package io.cattle.platform.host.stats.api;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.ArrayList;
import java.util.List;

public class StatsOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        boolean add = false;
        boolean hostStats = false;
        boolean containerStats = false;
        boolean project = false;

        if (original instanceof Host && DockerHostConstants.KIND_DOCKER.equals(((Host) original).getKind())) {
            add = true;
            hostStats = true;
            containerStats = true;
        } else if (original instanceof Instance && InstanceConstants.CONTAINER_LIKE.contains(((Instance) original).getKind())) {
            containerStats = true;
            add = true;
        } else if (original instanceof Account && AccountConstants.PROJECT_KIND.equals(((Account) original).getKind())) {
            project = true;
        } else if (original instanceof Service) {
            containerStats = true;
        }

        if (add) {
            converted.getLinks().put(StatsConstants.LINK_STATS, ApiContext.getUrlBuilder().resourceLink(converted, StatsConstants.LINK_STATS));
        }
        if (hostStats) {
            converted.getLinks().put(StatsConstants.HOST_STATS, ApiContext.getUrlBuilder().resourceLink(converted, StatsConstants.HOST_STATS));
        }
        if (containerStats) {
            converted.getLinks().put(StatsConstants.CONTAINER_STATS, ApiContext.getUrlBuilder().resourceLink(converted, StatsConstants.CONTAINER_STATS));
        }
        if (project) {
            converted.getLinks().put(StatsConstants.HOST_STATS,
                    ApiContext.getUrlBuilder().resourceLink(converted, "projects/" + converted.getId() + "/" + StatsConstants.HOST_STATS));
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        List<String> types = new ArrayList<>(InstanceConstants.CONTAINER_LIKE);
        types.add(HostConstants.TYPE);
        types.add(ProjectConstants.TYPE);
        types.add("service");
        types.add("loadBalancerService");
        return types.toArray(new String[types.size()]);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

}