package io.cattle.platform.api.instance;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.ContainerExec;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.Map;

public class ExecActionHandler implements ActionHandler {

    private static final DynamicStringProperty EXEC_AGENT_PATH = ArchaiusUtil.getString("exec.agent.path");

    HostApiService apiService;
    ObjectManager objectManager;

    public ExecActionHandler(HostApiService apiService, ObjectManager objectManager) {
        super();
        this.apiService = apiService;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {

        Host host = null;
        Instance instance = null;

        if (obj instanceof Instance) {
            instance = (Instance) obj;
            host = DockerUtils.getHostFromContainer(objectManager, instance, null);
        }

        if (host == null) {
            return null;
        }

        ContainerExec exec = request.proxyRequestObject(ContainerExec.class);

        String dockerId = DockerUtils.getDockerIdentifier(instance);
        Map<String, Object> data = CollectionUtils.asMap(InstanceConstants.DOCKER_ATTACH_STDIN, exec.getAttachStdin(),
                InstanceConstants.DOCKER_ATTACH_STDOUT, exec.getAttachStdout(), InstanceConstants.DOCKER_TTY, exec.getTty(),
                InstanceConstants.DOCKER_CMD, exec.getCommand(), InstanceConstants.DOCKER_CONTAINER, dockerId);

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("exec", data), EXEC_AGENT_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        return new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
    }


}