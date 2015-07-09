package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostPurge extends AbstractDefaultProcessHandler {

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        // TODO: Implement some bulk operation
        List<ClusterHostMapRecord> clusterHostMaps = Collections.emptyList();
        if (host.getKind().equals(ClusterConstants.KIND)) {
            clusterHostMaps = clusterHostMapDao.findClusterHostMapsForCluster(host);
        } else {
            clusterHostMaps = clusterHostMapDao.findClusterHostMapsHavingHost(host);
        }
        for (ClusterHostMapRecord mapping : clusterHostMaps) {
            objectProcessManager.scheduleProcessInstance(ClusterConstants.CLUSTER_HOST_MAP_REMOVE_PROCESS, mapping, null);
        }

        if (host.getAgentId() == null) {
            return null;
        }

        return null;
    }

}
