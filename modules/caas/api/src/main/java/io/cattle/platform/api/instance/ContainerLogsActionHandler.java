package io.cattle.platform.api.instance;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.ContainerLogs;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.Map;

public class ContainerLogsActionHandler implements ActionHandler {

    private static final DynamicStringProperty HOST_LOGS_PATH = ArchaiusUtil.getString("host.logs.path");

    HostApiService apiService;
    ObjectManager objectManager;

    public ContainerLogsActionHandler(HostApiService apiService, ObjectManager objectManager) {
        super();
        this.apiService = apiService;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {

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

        String dockerId = DockerUtils.getDockerIdentifier(container);
        Map<String, Object> data = CollectionUtils.asMap(InstanceConstants.DOCKER_CONTAINER, dockerId, "Lines", logs.getLines(), "Follow",
                logs.getFollow());

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("logs", data), HOST_LOGS_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        HostAccess access = new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
        return access;
    }

}
