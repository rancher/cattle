package io.cattle.platform.docker.api;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.ContainerProxy;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class ContainerProxyActionHandler implements ActionHandler {

    private static final DynamicStringProperty HOST_PROXY_PATH = ArchaiusUtil.getString("host.proxy.path");
    private static final DynamicLongProperty EXPIRE_SECONDS = ArchaiusUtil.getLong("host.proxy.jwt.expiration.seconds");

    @Inject
    HostApiService apiService;
    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.proxy";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Host host = null;
        Instance container = null;

        if (obj instanceof Instance) {
            container = (Instance) obj;
            host = DockerUtils.getHostFromContainer(objectManager, container, null);
        }

        if (host == null) {
            return null;
        }

        ContainerProxy proxy = request.proxyRequestObject(ContainerProxy.class);

        String dockerId = DockerUtils.getDockerIdentifier(container);
        String ipAddress = DataAccessor.fieldString(container, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        if (ipAddress == null) {
            return null;
        }

        Map<String, Object> data = CollectionUtils.asMap(DockerInstanceConstants.DOCKER_CONTAINER, dockerId,
                "address", ipAddress + ":" + proxy.getPort());

        Date expiration = new Date(System.currentTimeMillis() + EXPIRE_SECONDS.get() * 1000);

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("proxy", data),
                expiration, HOST_PROXY_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        HostAccess access = new HostAccess(apiAccess.getUrl().replaceFirst("ws", "http"), apiAccess.getAuthenticationToken());
        return access;
    }

}
