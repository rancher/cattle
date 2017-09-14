package io.cattle.platform.api.stats;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerStatsLinkHandler implements LinkHandler {

    HostApiService hostApiService;
    ObjectManager objectManager;

    public ContainerStatsLinkHandler(HostApiService hostApiService, ObjectManager objectManager) {
        super();
        this.hostApiService = hostApiService;
        this.objectManager = objectManager;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return StatsConstants.CONTAINER_STATS.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Host host = null;
        Instance instance = null;

        if (obj instanceof Instance) {
            instance = (Instance) obj;
            host = DockerUtils.getHostFromContainer(objectManager, instance);
        } else if (obj instanceof Host) {
            host = (Host) obj;
        }

        if (host == null) {
            return null;
        }

        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent == null || !CommonStatesConstants.ACTIVE.equals(agent.getState())) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> containerIdsMap = new HashMap<>();

        List<Instance> instances;
        if (instance == null) {
            instances = objectManager.children(host, Instance.class);
        } else {
            instances = new ArrayList<>();
            instances.add(instance);
        }
        for (Instance i : instances) {
            if (i.getRemoved() != null) {
                continue;
            }
            String dockerId = DockerUtils.getDockerIdentifier(i);
            //This is true if the container is in CREATING state, where it doesn't have a dockerId, but it is not in REMOVED state
            if (StringUtils.isEmpty(dockerId)) {
                continue;
            }
            containerIdsMap.put(dockerId, ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(i), i.getId()));
        }
        payload.put("containerIds", containerIdsMap);

        String[] segments = null;
        if (instance != null) {
            segments = new String[] { StatsConstants.CONTAINER_STATS_PATH.get(), DockerUtils.getDockerIdentifier(instance) };
        } else {
            segments = new String[] { StatsConstants.CONTAINER_STATS_PATH.get() };
        }

        HostApiAccess apiAccess = hostApiService.getAccess(request, host.getId(), payload, segments);
        if (apiAccess == null) {
            return null;
        }

        String url = apiAccess.getUrl();

        StatsAccess statsAccess = new StatsAccess();
        statsAccess.setToken(apiAccess.getAuthenticationToken());
        statsAccess.setAuthToken(apiAccess.getAuthenticationToken());
        statsAccess.setUrl(url);

        return statsAccess;
    }

}