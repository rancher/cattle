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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;

public class PingInstancesMonitorImpl implements PingInstancesMonitor {

    private static final DynamicLongProperty CACHE_TIME = ArchaiusUtil.getLong("ha.instance.state.cache.millis");
    private static final DynamicBooleanProperty COMPLAIN = ArchaiusUtil.getBoolean("ha.instance.state.report.unknown");

    private static final Logger log = LoggerFactory.getLogger(PingInstancesMonitorImpl.class);

    PingInstancesMonitorDao monitorDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    LoadingCache<Long, Set<String>> instanceCache = CacheBuilder.newBuilder()
                                                        .expireAfterWrite(CACHE_TIME.get(), TimeUnit.MILLISECONDS)
                                                        .build(new CacheLoader<Long, Set<String>>() {
                                                            @Override
                                                            public Set<String> load(Long key) throws Exception {
                                                                return PingInstancesMonitorImpl.this.load(key);
                                                            }
                                                        });

    @Override
    public void pingReply(Ping ping) {
        Set<String> instances = getInstances(ping);

        if ( instances == null ) {
            return;
        }

        long agentId = Long.parseLong(ping.getResourceId());
        Set<String> knownInstances = instanceCache.getUnchecked(agentId);

        if ( different(agentId, knownInstances, instances, true) ) {
            knownInstances = load(agentId);
            instanceCache.put(agentId, knownInstances);
            different(agentId, knownInstances, instances, false);
        }
    }

    @Override
    public void computeInstanceActivateReply(Event event) {
        Long agentId = monitorDao.getAgentIdForInstanceHostMap(event.getResourceId());
        if ( agentId != null ) {
            instanceCache.invalidate(agentId);
        }
    }



    protected boolean different(long agentId, Set<String> knownInstances, Set<String> instances, boolean check) {
        instances = new HashSet<String>(instances);
        for ( String instance : knownInstances ) {
            if ( ! instances.remove(instance) ) {
                if ( check ) {
                    return true;
                } else {
                    restart(instance);
                }
            }
        }

        if ( ! check && COMPLAIN.get() ) {
            for ( String instance : instances ) {
                log.error("Unknown instance [{}] reported from agent [{}]", instance, agentId);
            }
        }

        return false;
    }

    protected void restart(final String uuid) {
        Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
        Map<String,Object> data = new HashMap<String, Object>();
        DataAccessor.fromMap(data)
            .withScope(InstanceProcessOptions.class)
            .withKey(InstanceProcessOptions.HA_RESTART)
            .set(true);

        processManager.scheduleProcessInstance(InstanceConstants.PROCESS_RESTART, instance, data, new Predicate() {
            @Override
            public boolean evaluate(ProcessState state, ProcessInstance processInstance, ProcessDefinition definition) {
                Instance instance = objectManager.findOne(Instance.class, ObjectMetaDataManager.UUID_FIELD, uuid);
                return InstanceConstants.STATE_RUNNING.equals(instance.getState());
            }
        });
    }

    protected Set<String> getInstances(Ping ping) {
        PingData data = ping.getData();
        if ( data == null || ping.getResourceId() == null ) {
            return null;
        }

        List<Map<String,Object>> resources = data.getResources();
        if ( resources == null || ! ping.getOption(Ping.INSTANCES) ) {
            return null;
        }

        Set<String> instances = new HashSet<String>();

        for ( Map<String, Object> resource : resources ) {
            Object state = resource.get(ObjectMetaDataManager.STATE_FIELD);
            Object type = resource.get(ObjectMetaDataManager.TYPE_FIELD);
            Object uuid = resource.get(ObjectMetaDataManager.UUID_FIELD);

            if ( ! InstanceConstants.TYPE.equals(type) || ! InstanceConstants.STATE_RUNNING.equals(state) || uuid == null ) {
                continue;
            }

            instances.add(uuid.toString());
        }

        return instances;
    }

    protected Set<String> load(Long agentId) {
        return monitorDao.getHosts(agentId);
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
