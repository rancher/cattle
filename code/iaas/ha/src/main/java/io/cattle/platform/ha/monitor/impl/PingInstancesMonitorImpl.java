package io.cattle.platform.ha.monitor.impl;

import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.HostConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.process.instance.InstanceProcessOptions.*;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.process.Predicate;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.framework.event.data.PingData;
import io.cattle.platform.ha.monitor.PingInstancesMonitor;
import io.cattle.platform.ha.monitor.dao.PingInstancesMonitorDao;
import io.cattle.platform.ha.monitor.event.InstanceForceStop;
import io.cattle.platform.ha.monitor.model.KnownInstance;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.instance.InstanceProcessOptions;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.netflix.config.DynamicLongProperty;

public class PingInstancesMonitorImpl implements PingInstancesMonitor {

    private static final DynamicLongProperty CACHE_TIME = ArchaiusUtil.getLong("ha.instance.state.cache.millis");
    private static final DynamicLongProperty HOST_ID_CACHE_TIME = ArchaiusUtil.getLong("ha.host.id.cache.millis");

    private static final Logger log = LoggerFactory.getLogger(PingInstancesMonitorImpl.class);

    private static final String UKNOWN_OUT_OF_SYNC_WARNING = "Instance out of sync and can't determine action to take. Uuid [{}]. Docker id [{}]. "
            + "State in rancher [{}]. State on host [{}]";

    @Inject
    AgentDao agentDao;
    @Inject
    ObjectMetaDataManager objectMetaDataManager;
    @Inject
    AgentLocator agentLocator;
    @Inject
    LockDelegator lockDelegator;
    @Inject
    PingInstancesMonitorDao monitorDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;

    LoadingCache<Long, Map<String, KnownInstance>> instanceCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIME.get(), TimeUnit.MILLISECONDS)
            .build(new CacheLoader<Long, Map<String, KnownInstance>>() {
                @Override
                public Map<String, KnownInstance> load(Long key) throws Exception {
                    return PingInstancesMonitorImpl.this.load(key);
                }
            });

    LoadingCache<ImmutablePair<Long, String>, AgentAndHost> hostCache = CacheBuilder.newBuilder()
            .expireAfterWrite(HOST_ID_CACHE_TIME.get(), TimeUnit.MILLISECONDS).build(new CacheLoader<ImmutablePair<Long, String>, AgentAndHost>() {
                @Override
                public AgentAndHost load(ImmutablePair<Long, String> key) throws Exception {
                    return PingInstancesMonitorImpl.this.loadAgentAndHostData(key);
                }
            });

    @Override
    public void pingReply(Ping ping) {
        ReportedInstances reportedInstances = getInstances(ping);

        if (reportedInstances == null || StringUtils.isEmpty(reportedInstances.hostUuid))
            return;

        long agentId = Long.parseLong(ping.getResourceId());
        Map<String, KnownInstance> knownInstances = instanceCache.getUnchecked(agentId);

        AgentAndHost agentAndHost = null;
        try {
            agentAndHost = hostCache.getUnchecked(new ImmutablePair<Long, String>(agentId, reportedInstances.hostUuid));
        } catch (UncheckedExecutionException e) {
            // CantFindAgentAndHostException can be ignored because the host may not exist yet. Rethrow all other exceptions.
            if (!(e.getCause() instanceof CantFindAgentAndHostException)) {
                throw e;
            }
        }

        if (agentAndHost == null) {
            log.info("Couldn't find host with uuid [{}] for agent [{}]", reportedInstances.hostUuid, agentId);
            return;
        }

        try {
            syncContainers(knownInstances, reportedInstances, agentAndHost.agentAccountId, agentId, agentAndHost.hostId, true);
        } catch (ContainersOutOfSync e) {
            knownInstances = load(agentId);
            instanceCache.put(agentId, knownInstances);
            syncContainers(knownInstances, reportedInstances, agentAndHost.agentAccountId, agentId, agentAndHost.hostId, false);
        }
    }

    @Override
    public void computeInstanceActivateReply(Event event) {
        Long agentId = monitorDao.getAgentIdForInstanceHostMap(event.getResourceId());
        if (agentId != null) {
            instanceCache.invalidate(agentId);
        }
    }

    /*
     * If checkOnly is true, will raise a ContainersOutOfSync exception, indicating this should be reran with checkOnly set to false.
     */
    void syncContainers(Map<String, KnownInstance> knownInstances, ReportedInstances reportedInstances, long agentAccountId, long agentId, long hostId,
            boolean checkOnly) {
        Map<String, ReportedInstance> needsSynced = new HashMap<String, ReportedInstance>();
        Map<String, String> syncActions = new HashMap<String, String>();
        Set<String> needsHaRestart = new HashSet<String>();

        determineSyncActions(knownInstances, reportedInstances, needsSynced, syncActions, needsHaRestart, checkOnly);

        for (String uuid : needsHaRestart) {
            restart(uuid);
        }

        for (Map.Entry<String, ReportedInstance> syncEntry : needsSynced.entrySet()) {
            ReportedInstance ri = syncEntry.getValue();
            String syncAction = syncActions.get(syncEntry.getKey());
            if (EVENT_INSTANCE_FORCE_STOP.equals(syncAction)) {
                forceStop(ri.getExternalId(), agentId);
            } else if (HA_RESTART.equals(syncAction)) {
                String uuid = ri.getInstance().getUuid();
                restart(uuid);
            } else {
                scheduleContainerEvent(agentAccountId, hostId, ri, syncAction);
            }
        }
    }

    void determineSyncActions(Map<String, KnownInstance> knownInstances, ReportedInstances reportedInstances, Map<String, ReportedInstance> needsSynced,
            Map<String, String> syncActions, Set<String> needsHaRestart, boolean checkOnly) {
        Map<String, KnownInstance> inRancher = new HashMap<String, KnownInstance>(knownInstances);
        Map<String, ReportedInstance> onHost = new HashMap<String, ReportedInstance>(reportedInstances.byExternalId);
        for (Map.Entry<String, ReportedInstance> reported : reportedInstances.byUuid.entrySet()) {
            KnownInstance ki = knownInstances.get(reported.getKey());
            if (ki != null) {
                removeAndDetermineSyncAction(needsSynced, syncActions, checkOnly, inRancher, onHost, reported.getValue(), ki,
                        reported.getValue().getExternalId(), reported.getKey());
            }
        }

        if (!onHost.isEmpty() || !inRancher.isEmpty()) {
            Map<String, KnownInstance> knownByExternalId = new HashMap<String, KnownInstance>();
            for (KnownInstance ki : knownInstances.values()) {
                if (StringUtils.isNotEmpty(ki.getExternalId()))
                    knownByExternalId.put(ki.getExternalId(), ki);
            }

            for (Map.Entry<String, ReportedInstance> reported : reportedInstances.byExternalId.entrySet()) {
                KnownInstance ki = knownByExternalId.get(reported.getKey());
                if (ki != null) {
                    removeAndDetermineSyncAction(needsSynced, syncActions, checkOnly, inRancher, onHost, reported.getValue(), ki,
                            reported.getKey(), ki.getUuid());
                }
            }
        }

        // Anything left in onHost is on the host, but not in rancher.
        for (Map.Entry<String, ReportedInstance> create : onHost.entrySet()) {
            ReportedInstance ri = create.getValue();
            if (StringUtils.isNotEmpty(ri.getSystemContainer())) {
                // Unknown system container. Force stop.
                if (!STATE_STOPPED.equals(ri.getState())) {
                    addSyncAction(needsSynced, syncActions, ri, EVENT_INSTANCE_FORCE_STOP, checkOnly);
                }
                continue;
            }
            addSyncAction(needsSynced, syncActions, ri, EVENT_START, checkOnly);
        }

        // Anything left in inRancher is in rancher, but not on the host.
        for (KnownInstance ki : inRancher.values()) {
            List<String> forRemove = Arrays.asList(CommonStatesConstants.REMOVING, InstanceConstants.STATE_ERROR,
                    InstanceConstants.STATE_ERRORING);
            if (objectMetaDataManager.isTransitioningState(Instance.class, ki.getState()) || ki.getRemoved() != null
                    || forRemove.contains(ki.getState())
                    || (STATE_STOPPED.equals(ki.getState()) && StringUtils.isEmpty(ki.getExternalId())))
                continue;

            if (StringUtils.isNotEmpty(ki.getSystemContainer()) || StringUtils.isEmpty(ki.getExternalId()) || hasInstanceTriggeredStopConfigured(ki)) {
                // System container, not enough info to perform no-op action, or has an instance triggered stop policy. Schedule potential restart.
                // This is the one place we can't use addSyncAction, since we don't have (and can't construct) a ReportedInstance.
                if (!STATE_STOPPED.equals(ki.getState())) {
                    if (checkOnly) {
                        throw new ContainersOutOfSync();
                    }
                    needsHaRestart.add(ki.getUuid());
                }
            } else {
                ReportedInstance ri = new ReportedInstance();
                ri.setExternalId(ki.getExternalId());
                ri.setUuid(ki.getUuid());
                Object imageUuid = CollectionUtils.getNestedValue(ki.getData(), DataUtils.FIELDS, FIELD_IMAGE_UUID);
                String image = imageUuid != null ? imageUuid.toString() : null;
                ri.setImage(image);
                addSyncAction(needsSynced, syncActions, ri, EVENT_DESTROY, checkOnly);
            }
        }
    }

    boolean hasInstanceTriggeredStopConfigured(KnownInstance ki) {
        return StringUtils.isNotEmpty(ki.getInstanceTriggeredStop()) && !ON_STOP_STOP.equals(ki.getInstanceTriggeredStop());
    }

    void removeAndDetermineSyncAction(Map<String, ReportedInstance> needsSynced, Map<String, String> syncActions, boolean checkOnly,
            Map<String, KnownInstance> inRancher, Map<String, ReportedInstance> onHost, ReportedInstance reportedInstance, KnownInstance instance,
            String onHostKey, String inRancherKey) {
        onHost.remove(onHostKey);
        inRancher.remove(inRancherKey);
        reportedInstance.setInstance(instance);
        determineSyncAction(instance, reportedInstance, needsSynced, syncActions, checkOnly);
    }

    void determineSyncAction(KnownInstance ki, ReportedInstance ri, Map<String, ReportedInstance> needsSynced, Map<String, String> syncActions,
            boolean checkOnly) {
        if (objectMetaDataManager.isTransitioningState(Instance.class, ki.getState()) || StringUtils.equals(ki.getState(), ri.getState()))
            return;

        boolean sysCon = StringUtils.isNotEmpty(ki.getSystemContainer()) || StringUtils.isNotEmpty(ri.getSystemContainer());

        if (STATE_RUNNING.equals(ri.getState())) {
            // Container is running on host but not in Rancher. Take action
            if (ki.getRemoved() != null) {
                // If rancher thinks it's removed, send an explicit stop down to host.
                addSyncAction(needsSynced, syncActions, ri, EVENT_INSTANCE_FORCE_STOP, checkOnly);
            } else if (STATE_STOPPED.equals(ki.getState())) {
                // For system containers, rancher is source of truth, stop it. For user containers, do a no-op start to sync state.
                String doAction = sysCon ? EVENT_INSTANCE_FORCE_STOP : EVENT_START;
                addSyncAction(needsSynced, syncActions, ri, doAction, checkOnly);
            } else {
                log.warn(UKNOWN_OUT_OF_SYNC_WARNING, ki.getUuid(), ri.getExternalId(), ki.getState(), ri.getState());
            }
        } else if (STATE_RUNNING.equals(ki.getState())) {
            if (STATE_STOPPED.equals(ri.getState())) {
                // Container is running in Rancher, but is not running on host.
                // For system containers or containers with a instance triggered stop action, schedule HA_RESTART (stop and possible start based
                // on triggeredInstanceStop field). For user containers, a no-op stop to sync up.
                String doAction = sysCon || hasInstanceTriggeredStopConfigured(ki) ? HA_RESTART : EVENT_STOP;
                addSyncAction(needsSynced, syncActions, ri, doAction, checkOnly);
            } else {
                log.warn(UKNOWN_OUT_OF_SYNC_WARNING, ki.getUuid(), ri.getExternalId(), ki.getState(), ri.getState());
            }
        }
    }

    void addSyncAction(Map<String, ReportedInstance> needsSynced, Map<String, String> syncActions, ReportedInstance ri, String action, boolean checkOnly) {
        if (checkOnly) {
            throw new ContainersOutOfSync();
        }
        needsSynced.put(ri.getExternalId(), ri);
        syncActions.put(ri.getExternalId(), action);
    }

    void scheduleContainerEvent(Long agentId, Long hostId, ReportedInstance ri, String event) {
        if (StringUtils.isEmpty(ri.getImage()) || StringUtils.isEmpty(ri.getExternalId()) || StringUtils.isEmpty(ri.getUuid())) {
            log.error("Not enough information to schedule container event: [" + ri.toString() + "].");
            return;
        }
        ContainerEvent ce = objectManager.newRecord(ContainerEvent.class);
        ce.setAccountId(agentId);
        ce.setExternalFrom(ri.getImage());
        ce.setExternalId(ri.getExternalId());
        ce.setExternalStatus(event);
        ce.setExternalTimestamp(ri.getCreated());
        ce.setKind(CONTAINER_EVENT_KIND);
        ce.setHostId(hostId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(CONTAINER_EVENT_SYNC_NAME, ri.getUuid());
        data.put(CONTAINER_EVENT_SYNC_LABELS, ri.getLabels());
        ce = objectManager.create(ce);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, ce, data);
    }

    protected void forceStop(final String containerId, Long agentId) {
        final Event event = new InstanceForceStop(containerId);
        final RemoteAgent agent = agentLocator.lookupAgent(agentId);
        agent.publish(event);
    }

    protected void restart(final String uuid) {
        Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
        Map<String, Object> data = new HashMap<String, Object>();
        DataAccessor.fromMap(data).withScope(InstanceProcessOptions.class).withKey(HA_RESTART).set(true);

        processManager.scheduleProcessInstance(PROCESS_RESTART, instance, data, new Predicate() {
            @Override
            public boolean evaluate(ProcessState state, ProcessInstance processInstance, ProcessDefinition definition) {

                Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
                return STATE_RUNNING.equals(instance.getState());
            }
        });
    }

    protected ReportedInstances getInstances(Ping ping) {
        PingData data = ping.getData();

        if (data == null || ping.getResourceId() == null) {
            return null;
        }

        List<Map<String, Object>> resources = data.getResources();
        if (resources == null || !ping.getOption(Ping.INSTANCES)) {
            return null;
        }

        ReportedInstances reportedInstances = new ReportedInstances();

        for (Map<String, Object> resource : resources) {
            Object type = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.TYPE_FIELD).as(String.class);

            if (FIELD_HOST_UUID.equals(type))
                reportedInstances.hostUuid = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.UUID_FIELD).as(String.class);

            if (!InstanceConstants.TYPE.equals(type))
                continue;

            ReportedInstance ri = new ReportedInstance(resource);
            if (!StringUtils.equals("rancher-agent", ri.getSystemContainer())) {
                reportedInstances.byUuid.put(ri.getUuid(), ri);
                reportedInstances.byExternalId.put(ri.getExternalId(), ri);
            }
        }

        return reportedInstances;
    }

    protected class ContainersOutOfSync extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    protected AgentAndHost loadAgentAndHostData(ImmutablePair<Long, String> agentIdAndHostUuid) {
        Long agentId = agentIdAndHostUuid.left;
        String hostUuid = agentIdAndHostUuid.right;

        Agent agent = objectManager.loadResource(Agent.class, agentId);
        Host host = null;
        Map<String, Host> hosts = null;
        if (agent != null) {
            hosts = agentDao.getHosts(agent.getId());
            host = hosts.get(hostUuid);
        }

        if (agent == null || host == null)
            throw new CantFindAgentAndHostException();

        return new AgentAndHost(agent.getAccountId(), host.getId());
    }

    protected Map<String, KnownInstance> load(Long agentId) {
        if (agentId == null) {
            return new HashMap<String, KnownInstance>();
        }
        return monitorDao.getInstances(agentId.longValue());
    }

    private class AgentAndHost {
        Long agentAccountId;
        Long hostId;

        AgentAndHost(Long agentAccountId, Long hostId) {
            this.agentAccountId = agentAccountId;
            this.hostId = hostId;
        }
    }

    private class CantFindAgentAndHostException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
    }
}
