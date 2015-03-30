package io.cattle.iaas.cluster.process;

import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.ClusterHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ClusterHostMapRemoveHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    ClusterManager clusterManager;

    @Inject
    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "clusterhostmap.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Long clusterId = ((ClusterHostMap) state.getResource()).getClusterId();
        Long hostId = ((ClusterHostMap) state.getResource()).getHostId();

        Host cluster = objectManager.loadResource(Host.class, clusterId);
        Long managingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);

        if (hostId.equals(managingHostId) && CommonStatesConstants.ACTIVE.equals(cluster.getState())) {
            // deactivate to shutdown existing cluster server.
            // reactivate to pick new managing host and start up cluster server.
            objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, cluster,
                    ProcessUtils.chainInData(new HashMap<String, Object>(),
                            ClusterConstants.PROCESS_DEACTIVATE, ClusterConstants.PROCESS_ACTIVATE));
        } else {
            clusterManager.updateClusterServerConfig(state, cluster);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}