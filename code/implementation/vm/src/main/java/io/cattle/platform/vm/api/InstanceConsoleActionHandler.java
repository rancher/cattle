package io.cattle.platform.vm.api;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class InstanceConsoleActionHandler implements ActionHandler {

    private static final DynamicStringProperty CONSOLE_AGENT_PATH = ArchaiusUtil.getString("console.agent.path");

    @Inject
    HostApiService apiService;
    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.console";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Instance)) {
            return null;
        }

        Host host = null;
        Instance instance = null;

        if (obj instanceof Instance) {
            instance = (Instance)obj;
            host = DockerUtils.getHostFromContainer(objectManager, instance, null);
        }

        if (host == null) {
            return null;
        }

        String dockerId = DockerUtils.getDockerIdentifier(instance);
        Map<String, Object> data = CollectionUtils.asMap("container", dockerId);

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("console", data), CONSOLE_AGENT_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        return new HostAccess(apiAccess.getUrl(), apiAccess.getAuthenticationToken());
    }
}
