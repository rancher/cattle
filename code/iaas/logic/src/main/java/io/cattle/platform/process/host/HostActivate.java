package io.cattle.platform.process.host;

import io.cattle.iaas.cluster.process.lock.ClusterLock;
import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostActivate extends AbstractDefaultProcessHandler {
    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Inject
    ClusterManager clusterManager;

    @Inject
    LockManager lockManager;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        if (objectManager.isKind(state.getResource(), ClusterConstants.KIND)) {
            return handleCluster(state, process);
        } else {
            return handleHost(state, process);
        }
    }

    private HandlerResult handleCluster(final ProcessState state, ProcessInstance process) {
        final Host cluster = (Host)state.getResource();
        lockManager.lock(new ClusterLock(cluster), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                clusterManager.activateCluster(state, cluster);
            }
        });

        return null;
    }

    private HandlerResult handleHost(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        if (host.getAgentId() == null) {
            return null;
        }

        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent == null) {
            return null;
        }

        activate(agent, state.getData());

        // update cluster server configs for all clusters having having this host
        List<ClusterHostMapRecord> mappings = clusterHostMapDao.findClusterHostMapsHavingHost(host);
        for (ClusterHostMapRecord mapping : mappings) {
            // TODO: Bulk load clusters
            Host cluster = objectManager.loadResource(Host.class,  mapping.getClusterId());
            clusterManager.updateClusterServerConfig(state, cluster);
        }
        return null;
    }
}
