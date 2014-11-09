package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.HostStatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class HostStatsLinkHandler implements LinkHandler {

    private static final DynamicStringProperty HOST_STATS_SCHEME = ArchaiusUtil.getString("host.stats.scheme");
    private static final DynamicStringProperty HOST_STATS_PORT = ArchaiusUtil.getString("host.stats.port");
    private static final DynamicStringProperty HOST_STATS_PATH = ArchaiusUtil.getString("host.stats.path");

    HostApiService hostApiService;
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE_CONTAINER, HostConstants.TYPE };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return HostStatsConstants.LINK_STATS.equals(link);
    }

    protected Host getHostFromContainer(Instance instance) {
        Host found = null;
        for ( Host host : objectManager.mappedChildren(instance, Host.class) ) {
            found = host;
        }

        if ( found != null ) {
            found = ApiUtils.getPolicy().authorizeObject(found);
        }

        if ( found == null ) {
            return null;
        }

        if ( ! DockerHostConstants.KIND_DOCKER.equals(found.getKind()) ) {
            return null;
        }

        return found;
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Host host = null;
        Instance instance = null;

        if (obj instanceof Instance) {
            instance = (Instance) obj;
            host = getHostFromContainer(instance);
        } else if (obj instanceof Host) {
            host = (Host) obj;
        }

        if ( host == null ) {
            return null;
        }

        HostApiAccess apiAccess = hostApiService.getAccess(host.getId());
        if ( apiAccess == null ) {
            return null;
        }

        StringBuilder url = new StringBuilder(HOST_STATS_SCHEME.get());
        url.append("://").append(apiAccess.getHostname()).append(":").append(HOST_STATS_PORT.get());
        url.append(HOST_STATS_PATH.get());

        if ( instance != null ) {
            url.append("/").append(instance.getUuid());
        }

        StatsAccess statsAccess = new StatsAccess();
        statsAccess.setToken(apiAccess.getAuthenticationToken());
        statsAccess.setUrl(url.toString());

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
