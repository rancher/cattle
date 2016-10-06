package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRemove extends AbstractDefaultProcessHandler {

    @Inject
    InstanceDao instanceDao;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent == null) {
            return null;
        }

        removeInstances(host);

        PhysicalHost physHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (physHost != null) {
            deactivateThenScheduleRemove(physHost, null);
        }

        return null;
    }

    protected void removeInstances(Host host) {
        for (Instance instance : instanceDao.getNonRemovedInstanceOn(host.getId())) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                        CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
            }
        }
    }

}
