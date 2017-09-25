package io.cattle.platform.api.dial;

import io.cattle.platform.api.instance.DockerSocketProxyActionHandler;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.core.addon.Dial;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import static io.cattle.platform.core.model.Tables.*;

public class DialManager extends AbstractNoOpResourceManager {

    HostApiService apiService;
    ObjectManager objectManager;

    public DialManager(HostApiService apiService, ObjectManager objectManager) {
        this.apiService = apiService;
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Dial dial = request.proxyRequestObject(Dial.class);

        Long clusterId = dial.getClusterId();
        if (clusterId == null) {
            return null;
        }

        Host host = getHost(dial.getAddress(), clusterId);
        if (host == null) {
            return null;
        }

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(),
                CollectionUtils.asMap(
                        "proto", "tcp",
                        "address", normalizeAddress(dial.getAddress())),
                DockerSocketProxyActionHandler.SOCKET_PROXY_PATH.get());
        if (apiAccess == null) {
            return null;
        }

        return new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
    }

    protected String normalizeAddress(String address) {
        if (NetUtils.isIpPort(address)) {
            return address;
        }
        String[] parts = address.split(":");
        if (parts.length != 2) {
            return address;
        }

        return "localhost:" + parts[1];
    }

    protected Host getHost(String address, long clusterId) {
        if (NetUtils.isIpPort(address)) {
            return getFirstHost(clusterId);
        }
        return getHostByName(address, clusterId);
    }

    protected Host getFirstHost(long clusterId) {
        return objectManager.findAny(Host.class,
                HOST.CLUSTER_ID, clusterId,
                HOST.STATE, CommonStatesConstants.ACTIVE,
                HOST.AGENT_STATE, CommonStatesConstants.ACTIVE);
    }

    protected Host getHostByName(String address, long clusterId) {
        address = address.split(":")[0];
        for (Host host : objectManager.find(Host.class,
                HOST.CLUSTER_ID, clusterId,
                HOST.STATE, CommonStatesConstants.ACTIVE,
                HOST.AGENT_STATE, CommonStatesConstants.ACTIVE)) {

            if (address.equalsIgnoreCase(DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME))) {
                return host;
            }

            if (address.equalsIgnoreCase(DataAccessor.fieldString(host, HostConstants.FIELD_NODE_NAME))) {
                return host;
            }
        }

        return null;
    }
}
