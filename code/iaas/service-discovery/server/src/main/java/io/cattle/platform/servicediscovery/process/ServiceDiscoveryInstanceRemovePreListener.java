package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Named;

/**
 * This handler takes care of removing service-instance link on instance purge
 * The reason why link is removed on purge, not remove is - removed instance linked to service, might get restored, and
 * in this case it should continue be a part of the service
 * Service link to removed instance can be removed only if activate/update operation is called on the service
 * - separate handler takes care of that
 */
@Named
public class ServiceDiscoveryInstanceRemovePreListener extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {
    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.remove" };
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
            if (!(map.getState().equals(CommonStatesConstants.REMOVED) || map.getState().equals(
                    CommonStatesConstants.REMOVING))) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, map, null);
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
