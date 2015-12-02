package io.cattle.platform.docker.api;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.ContainerExec;
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

public class ExecActionHandler implements ActionHandler {

    private static final DynamicStringProperty EXEC_AGENT_PATH = ArchaiusUtil.getString("exec.agent.path");

    HostApiService apiService;
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.execute";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {

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
        Map<String, Object> data = CollectionUtils.asMap(DockerInstanceConstants.DOCKER_ATTACH_STDIN, exec.getAttachStdin(),
                DockerInstanceConstants.DOCKER_ATTACH_STDOUT, exec.getAttachStdout(), DockerInstanceConstants.DOCKER_TTY, exec.getTty(),
                DockerInstanceConstants.DOCKER_CMD, exec.getCommand(), DockerInstanceConstants.DOCKER_CONTAINER, dockerId);

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("exec", data), EXEC_AGENT_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        return new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
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