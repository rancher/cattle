package io.cattle.platform.process.host;

import io.cattle.iaas.cluster.process.lock.ClusterLock;
import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.process.lock.AgentCleanupLock;
import io.cattle.platform.process.util.ProcessHelpers;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostDeactivate extends AbstractDefaultProcessHandler {

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
        deactivateCluster((Host) state.getResource(), true);
        return null;
    }

    private HandlerResult handleHost(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        // update clusters having this host especially if this host was managing the cluster
        List<ClusterHostMapRecord> mappings = clusterHostMapDao.findClusterHostMapsHavingHost(host);
        for (ClusterHostMapRecord mapping : mappings) {
            // TODO: Bulk load clusters
            Host cluster = objectManager.loadResource(Host.class,  mapping.getClusterId());
            Long managingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);

            if (host.getId().equals(managingHostId)) {
                // deactivate to shutdown existing cluster server.
                // reactivate to pick new managing host and start up cluster server.
                deactivateCluster(cluster, false);
                clusterManager.activateCluster(state, cluster);
            } else {
                clusterManager.updateClusterServerConfig(state, cluster);
            }
        }

        return null;
    }

    private void deactivateCluster(final Host cluster, final boolean shouldSchedule) {
        lockManager.lock(new ClusterLock(cluster), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Long managingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
                if (managingHostId == null) {
                    return;
                }

                final Agent clusterServerAgent = clusterManager.getClusterServerAgent(cluster);
                if (clusterServerAgent == null) {
                    return;
                }

                Instance clusterServerInstance = clusterManager.getClusterServerInstance(cluster);

                // TODO: This should really be moved to AgentDeactivate (along with the agent cleanup code below for a host)
                lockManager.lock(new AgentCleanupLock(clusterServerAgent.getId()), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        List<Host> children = ProcessHelpers.getNonRemovedChildren(objectManager, clusterServerAgent, Host.class);
                        if ( children.size() > 1 ) {
                            return;
                        }

                        try {
                            if (shouldSchedule) {
                                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, clusterServerAgent,
                                        null);
                            } else {
                                objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, clusterServerAgent,
                                        null);
                            }
                        } catch (ProcessCancelException e) {
                            if (shouldSchedule) {
                                objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE,
                                        clusterServerAgent,
                                        ProcessUtils.chainInData(new HashMap<String, Object>(), AgentConstants.PROCESS_DEACTIVATE,
                                                AgentConstants.PROCESS_REMOVE));
                            } else {
                                objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE,
                                        clusterServerAgent,
                                        ProcessUtils.chainInData(new HashMap<String, Object>(), AgentConstants.PROCESS_DEACTIVATE,
                                                AgentConstants.PROCESS_REMOVE));
                            }
                        }
                    }
                });

                if (clusterServerInstance != null) {
                    // try to remove first
                    try {
                        if (shouldSchedule) {
                            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, clusterServerInstance,
                                null);
                        } else {
                            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, clusterServerInstance,
                                null);
                        }
                    } catch (ProcessCancelException e) {
                        if (shouldSchedule) {
                            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, clusterServerInstance,
                                    CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true, InstanceConstants.DEALLOCATE_OPTION, true));
                        } else {
                            objectProcessManager.executeProcess(InstanceConstants.PROCESS_STOP, clusterServerInstance,
                                    CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true, InstanceConstants.DEALLOCATE_OPTION, true));
                        }
                    }
                }
                DataUtils.getWritableFields(cluster).put(ClusterConstants.MANAGING_HOST, null);
                objectManager.persist(cluster);
            }
        });
    }
}
