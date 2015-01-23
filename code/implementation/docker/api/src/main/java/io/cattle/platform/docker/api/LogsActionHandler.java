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

import com.netflix.config.DynamicStringProperty;

public class LogsActionHandler implements ActionHandler {
    
    private static final DynamicStringProperty LOGS_CONSOLE_SCHEME = ArchaiusUtil.getString("console.logs.scheme");
    private static final DynamicStringProperty LOGS_CONSOLE_PORT = ArchaiusUtil.getString("console.logs.port");
    private static final DynamicStringProperty LOGS_CONSOLE_PATH = ArchaiusUtil.getString("console.logs.path");

    HostApiService apiService;
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.logs";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        
        Host host = null;
        Instance instance = null;

        if (obj instanceof Instance) {
            instance = (Instance) obj;
            host = DockerUtils.getHostFromContainer(objectManager, instance, null);
        }

        if ( host == null ) {
            return null;
        }

        ContainerLogs logs = request.proxyRequestObject(ContainerLogs.class);

        Map<String,Object> data = CollectionUtils.asMap(
                DockerInstanceConstants.CONTAINER_LOGS_FOLLOW, logs.getFollow(),
                DockerInstanceConstants.CONTAINER_LOGS_TAIL, logs.getLines(),
                DockerInstanceConstants.DOCKER_CONTAINER, instance.getUuid());

        HostApiAccess apiAccess = apiService.getAccess(host.getId(),
                CollectionUtils.asMap("logs", data));

        if ( apiAccess == null ) {
            return null;
        }

        StringBuilder url = new StringBuilder(LOGS_CONSOLE_SCHEME.get());
        url.append("://").append(apiAccess.getHostname()).append(":").append(LOGS_CONSOLE_PORT.get());
        url.append(LOGS_CONSOLE_PATH.get());

        return new HostAccess(url.toString(), apiAccess.getAuthenticationToken());
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