package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ContainerStatsLinkHandler implements LinkHandler {

    @Inject
    HostApiService hostApiService;
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        List<String> types = new ArrayList<>(InstanceConstants.CONTAINER_LIKE);
        types.add(HostConstants.TYPE);
        return types.toArray(new String[types.size()]);
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
            throw new IllegalStateException();
        }

        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent == null || !CommonStatesConstants.ACTIVE.equals(agent.getState())) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> containerIdsMap = new HashMap<String, Object>();

        List<Instance> instances;
        if (instance == null) {
            instances = objectManager.mappedChildren(host, Instance.class);
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
        statsAccess.setUrl(url);

        return statsAccess;
    }

}