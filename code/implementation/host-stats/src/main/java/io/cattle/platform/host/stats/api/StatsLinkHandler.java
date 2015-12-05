package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

@Deprecated
public class StatsLinkHandler implements LinkHandler {

    private static final DynamicStringProperty HOST_STATS_PATH = ArchaiusUtil.getString("host.stats.path");

    HostApiService hostApiService;
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        List<String> types = new ArrayList<>(InstanceConstants.CONTAINER_LIKE);
        types.add(HostConstants.TYPE);
        return types.toArray(new String[types.size()]);
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return StatsConstants.LINK_STATS.equals(link);
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

        String[] pathSegments = null;
        if (instance != null) {
            pathSegments = new String[] { HOST_STATS_PATH.get(), DockerUtils.getDockerIdentifier(instance) };
        } else {
            pathSegments = new String[] { HOST_STATS_PATH.get() };
        }

        HostApiAccess apiAccess = hostApiService.getAccess(request, host.getId(), Collections.<String, Object> emptyMap(), pathSegments);
        if (apiAccess == null) {
            return null;
        }

        StatsAccess statsAccess = new StatsAccess();
        statsAccess.setToken(apiAccess.getAuthenticationToken());
        statsAccess.setUrl(apiAccess.getUrl());

        return statsAccess;
    }

    public HostApiService getHostApiService() {
        return hostApiService;
    }

    @Inject
    public void setHostApiService(HostApiService hostApiService) {
        this.hostApiService = hostApiService;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
