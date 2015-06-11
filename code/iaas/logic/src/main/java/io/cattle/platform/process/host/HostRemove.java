package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRemove extends AbstractDefaultProcessHandler {

    @Inject
    InstanceDao instanceDao;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        if (host.getAgentId() == null) {
            return null;
        }

        removeInstances(host);

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
