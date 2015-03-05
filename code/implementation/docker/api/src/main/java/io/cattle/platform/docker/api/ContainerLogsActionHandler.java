package io.cattle.platform.docker.api;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.ContainerLogs;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class ContainerLogsActionHandler implements ActionHandler {

    private static final DynamicStringProperty HOST_LOGS_SCHEME = ArchaiusUtil.getString("host.logs.scheme");
    private static final DynamicIntProperty HOST_LOGS_PORT = ArchaiusUtil.getInt("host.logs.port");
    private static final DynamicStringProperty HOST_LOGS_PATH = ArchaiusUtil.getString("host.logs.path");

    HostApiService apiService;
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.logs";
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

        ContainerLogs logs = request.proxyRequestObject(ContainerLogs.class);

        Map<String, Object> data = CollectionUtils.asMap(DockerInstanceConstants.DOCKER_CONTAINER, container.getUuid(), "Lines", logs.getLines(), "Follow",
                logs.getFollow());

        HostApiAccess apiAccess = apiService.getAccess(host.getId(), HOST_LOGS_PORT.get(), CollectionUtils.asMap("logs", data));

        if (apiAccess == null) {
            return null;
        }

        StringBuilder url = new StringBuilder((HOST_LOGS_SCHEME).get());
        url.append("://").append(apiAccess.getHostAndPort());
        url.append(HOST_LOGS_PATH.get());
        HostAccess access = new HostAccess(url.toString(), apiAccess.getAuthenticationToken());
        return access;
    }

    public HostApiService getApiService() {
        return apiService;
    }

    @Inject
    public void setApiService(HostApiService apiService) {
        this.apiService = apiService;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }
}
