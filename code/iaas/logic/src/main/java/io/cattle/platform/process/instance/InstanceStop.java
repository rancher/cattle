package io.cattle.platform.process.instance;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceStop extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    AllocatorService allocatorService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance) state.getResource();

        Map<String, Object> result = new ConcurrentHashMap<String, Object>();

        compute(instance, state);

        network(instance);

        storage(instance);

        allocatorService.ensureResourcesReleasedForStop(instance);

        return new HandlerResult(result);
    }

    protected void storage(Instance instance) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for (Volume volume : volumes) {
            if (volume.getRemoved() == null && !volume.getState().equals(CommonStatesConstants.REMOVED)) {
                deactivate(volume, null);
            }
        }
    }

    protected void network(Instance instance) {
        List<Nic> nics = getObjectManager().children(instance, Nic.class);

        for (Nic nic : nics) {
            if (nic.getRemoved() == null && !nic.getState().equals(CommonStatesConstants.REMOVED)) {
                deactivate(nic, null);
            }
        }

        for (Port port : getObjectManager().children(instance, Port.class)) {
            if (port.getRemoved() == null && !port.getState().equals(CommonStatesConstants.REMOVED)) {
                deactivate(port, null);
            }
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_INSTANCE_ID)) {
            if (link.getRemoved() == null && !link.getState().equals(CommonStatesConstants.REMOVED)) {
                deactivate(link, null);
            }
        }
    }

    protected void compute(Instance instance, ProcessState state) {
        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            if (map.getRemoved() == null) {
                try {
                    deactivate(map, state.getData());
                } catch (ProcessCancelException e) {
                    /* We ignore requested ihm because that means we allocated the instance but for
                     * whatever reason we never activated it (server crash, message lost, etc).  In this
                     * situation we just ignore it.
                     */
                    if (!CommonStatesConstants.REQUESTED.equals(map.getState())) {
                        throw e;
                    }
                }
            }
        }
    }
}
