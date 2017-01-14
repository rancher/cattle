package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ServiceContainerStatsLinkHandler implements LinkHandler {

    @Inject
    HostApiService hostApiService;
    @Inject
    ObjectManager objectManager;
    @Inject
    TokenService tokenService;

    private static final String SERVICE_PATH = "/service/";

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService" };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return StatsConstants.CONTAINER_STATS.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Service service = (Service) obj;

        if (service == null) {
            return null;
        }

        List<Instance> serviceInstances = objectManager.mappedChildren(service, Instance.class);

        String metaUrl = null;
        StatsAccess meta = new StatsAccess();
        List<StatsAccess> serviceStatsQuery = new ArrayList<>();
        for (Instance instance : serviceInstances) {
            List<String> invalidStates = Arrays.asList(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED,
                    InstanceConstants.STATE_ERRORING, InstanceConstants.STATE_ERROR);
            if (instance.getRemoved() != null || invalidStates.contains(instance.getState())) {
                continue;
            }
            String dockerId = DockerUtils.getDockerIdentifier(instance);
            //This is true if the container is in CREATING state, where it doesn't have a dockerId, but it is not in REMOVED state
            if (StringUtils.isEmpty(dockerId)) {
                continue;
            }
            Host host = DockerUtils.getHostFromContainer(objectManager, instance);
            if (host == null) {
                // host can be null when initial instance.start failed, so its being stopped/error out as a result
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> containerIdsMap = new HashMap<String, Object>();
            containerIdsMap.put(DockerUtils.getDockerIdentifier(instance),
                    ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(instance), instance.getId()));
            payload.put("containerIds", containerIdsMap);
            HostApiAccess apiAccess = hostApiService.getAccess(request, host.getId(), payload, new String[] { StatsConstants.CONTAINER_STATS_PATH.get() });
            if (apiAccess == null) {
                continue;
            }

            StringBuilder url = new StringBuilder();

            url.append(apiAccess.getUrl());

            metaUrl = url.toString() + SERVICE_PATH;

            url.append("/").append(dockerId);

            StatsAccess statsAccess = new StatsAccess();
            statsAccess.setToken(apiAccess.getAuthenticationToken());
            statsAccess.setUrl(url.toString());
            serviceStatsQuery.add(statsAccess);

        }
        Map<String, Object> metaQueryPayload = new HashMap<>();
        metaQueryPayload.put("service", serviceStatsQuery);

        meta.setToken(tokenService.generateToken(metaQueryPayload));
        meta.setUrl(metaUrl);

        return meta;
    }

}