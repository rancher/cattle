package io.cattle.platform.ha.monitor.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
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
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.instance.InstanceProcessOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.config.DynamicLongProperty;

public class PingInstancesMonitorImpl implements PingInstancesMonitor {

    private static final DynamicLongProperty CACHE_TIME = ArchaiusUtil.getLong("ha.instance.state.cache.millis");

    PingInstancesMonitorDao monitorDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    LoadingCache<Long, Map<String, String>> instanceCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIME.get(), TimeUnit.MILLISECONDS).build(
            new CacheLoader<Long, Map<String, String>>() {
                @Override
                public Map<String, String> load(Long key) throws Exception {
                    return PingInstancesMonitorImpl.this.load(key);
                }
            });

    @Override
    public void pingReply(Ping ping) {
        ReportedInstances reportedInstances = getInstances(ping);

        if (reportedInstances == null) {
            return;
        }

        long agentId = Long.parseLong(ping.getResourceId());
        Map<String, String> knownInstances = instanceCache.getUnchecked(agentId);

        if (handleUnreportedKnownInstances(agentId, knownInstances, reportedInstances, true)) {
            knownInstances = load(agentId);
            instanceCache.put(agentId, knownInstances);
            handleUnreportedKnownInstances(agentId, knownInstances, reportedInstances, false);
        }
    }

    @Override
    public void computeInstanceActivateReply(Event event) {
        Long agentId = monitorDao.getAgentIdForInstanceHostMap(event.getResourceId());
        if (agentId != null) {
            instanceCache.invalidate(agentId);
        }
    }

    protected boolean handleUnreportedKnownInstances(long agentId, Map<String, String> knownInstances,
            ReportedInstances reportedInstances, boolean checkOnly) {

        if (knownInstances != null) {
            for (Map.Entry<String, String> knownInstance : knownInstances.entrySet()) {
                if(!reportedInstances.byUuid.contains(knownInstance.getKey())
                        && !reportedInstances.byExternalId.contains(knownInstance.getValue())) {
                    if (checkOnly) {
                        return true;
                    } else {
                        // If this known instance was not reported, schedule a potential restart.
                        restart(knownInstance.getKey());
                    }
                }
            }
        }

        return false;
    }

    protected void restart(final String uuid) {
        Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
        Map<String, Object> data = new HashMap<String, Object>();
        DataAccessor.fromMap(data).withScope(InstanceProcessOptions.class).withKey(InstanceProcessOptions.HA_RESTART).set(true);

        processManager.scheduleProcessInstance(InstanceConstants.PROCESS_RESTART, instance, data, new Predicate() {
            @Override
            public boolean evaluate(ProcessState state, ProcessInstance processInstance, ProcessDefinition definition) {
                Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
                return InstanceConstants.STATE_RUNNING.equals(instance.getState());
            }
        });
    }

    protected ReportedInstances getInstances(Ping ping) {
        PingData data = ping.getData();

        if ( data == null || ping.getResourceId() == null ) {
            return null;
        }

        List<Map<String, Object>> resources = data.getResources();
        if ( resources == null || !ping.getOption(Ping.INSTANCES) ) {
            return null;
        }

        ReportedInstances reportedInstances = new ReportedInstances();

        for ( Map<String, Object> resource : resources ) {
            Object state = resource.get(ObjectMetaDataManager.STATE_FIELD);
            Object type = resource.get(ObjectMetaDataManager.TYPE_FIELD);
            Object uuid = resource.get(ObjectMetaDataManager.UUID_FIELD);
            Object externalId = resource.get("dockerId");

            if ( !InstanceConstants.TYPE.equals(type) || !InstanceConstants.STATE_RUNNING.equals(state) ) {
                continue;
            }

            if ( uuid != null ) {
                reportedInstances.byUuid.add(uuid.toString());
            }

            if ( externalId != null ) {
                reportedInstances.byExternalId.add(externalId.toString());
            }
        }

        return reportedInstances;
    }

    protected class ReportedInstances {
        Set<String> byUuid = new HashSet<String>();
        Set<String> byExternalId = new HashSet<String>();
    }

    protected Map<String, String> load(Long agentId) {
        if (agentId == null) {
            return new HashMap<String, String>();
        }
        return monitorDao.getInstances(agentId.longValue());
    }

    public PingInstancesMonitorDao getMonitorDao() {
        return monitorDao;
    }

    @Inject
    public void setMonitorDao(PingInstancesMonitorDao monitorDao) {
        this.monitorDao = monitorDao;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
