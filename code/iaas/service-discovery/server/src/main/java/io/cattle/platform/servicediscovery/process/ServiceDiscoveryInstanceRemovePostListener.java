package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Named;

@Named
public class ServiceDiscoveryInstanceRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {
    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        deleteServiceMappings(instance);
        return null;
    }

    private void deleteServiceMappings(Instance instance) {
        List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                objectManager.loadResource(Instance.class, instance.getId()),
                ServiceExposeMap.class);
        for (ServiceExposeMap map : maps) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_INSTANCE_MAP_REMOVE,
                    map, null);
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
