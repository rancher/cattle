package io.cattle.platform.docker.api;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.HashMap;

import com.netflix.config.DynamicStringProperty;

public class DockerSocketProxyActionHandler implements ActionHandler {

    private static final DynamicStringProperty SOCKET_PROXY_PATH = ArchaiusUtil.getString("host.socketproxy.path");

    HostApiService apiService;
    ObjectManager objectManager;

    public DockerSocketProxyActionHandler(HostApiService apiService, ObjectManager objectManager) {
        super();
        this.apiService = apiService;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (obj == null) {
            return null;
        }

        Host host = (Host)obj;

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), new HashMap<String, Object>(), SOCKET_PROXY_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        HostAccess access = new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
        return access;
    }

}