package io.cattle.platform.process.externalevent;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.docker.process.lock.DockerStoragePoolVolumeCreateLock;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ExternalEventCreate extends AbstractDefaultProcessHandler {

    public static final String FIELD_AGENT_ID = "agentId";

    private static final Logger log = LoggerFactory.getLogger(ExternalEventCreate.class);

    @Inject
    AccountDao accountDao;
    @Inject
    LockManager lockManager;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    StoragePoolDao storagePoolDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    HostDao hostDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ExternalEvent event = (ExternalEvent)state.getResource();

        if (StringUtils.isEmpty(event.getExternalId())) {
            log.debug("External event doesn't have an external id: {}", event.getId());
            return null;
        }

        if (ExternalEventConstants.KIND_VOLUME_EVENT.equals(event.getKind())) {
            handleVolumeEvent(event, state, process);
        } else if (ExternalEventConstants.KIND_STORAGE_POOL_EVENT.equals(event.getKind())) {
            handleStoragePoolEvent(event, state, process);
        } else if (ExternalEventConstants.KIND_EXTERNAL_DNS_EVENT.equals(event.getKind())) {
            handleExternalDnsEvent(event, state, process);
        }

        return null;
    }

    // TODO We don't use this logic. Consider completely removing it.
    protected void handleVolumeEvent(final ExternalEvent event, ProcessState state, ProcessInstance process) {
        //new DockerStoragePoolVolumeCreateLock(storagePool, dVol.getUri())
        Object driver = CollectionUtils.getNestedValue(DataUtils.getFields(event), FIELD_VOLUME, VolumeConstants.FIELD_VOLUME_DRIVER);
        final Object name = CollectionUtils.getNestedValue(DataUtils.getFields(event), FIELD_VOLUME, FIELD_NAME);
        if (driver == null || name == null) {
            log.warn("Driver or volume name not specified. Returning. Event: {}", event);
            return;
        }
        String driverName = driver.toString();
        List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(event.getAccountId(), driverName);
        if (pools.size() == 0) {
            log.warn("Unknown storage pool. Returning. Driver name: {}", driverName);
            return;
        }
        final StoragePool storagePool = pools.get(0);

        lockManager.lock(new DockerStoragePoolVolumeCreateLock(storagePool, event.getExternalId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Volume volume = volumeDao.findVolumeByExternalId(storagePool.getId(), event.getExternalId());
                switch (event.getEventType()) {
                case ExternalEventConstants.TYPE_VOLUME_CREATE:
                    if (volume == null) {
                        Map<String, Object> volumeData = CollectionUtils.toMap(DataUtils.getFields(event).get(FIELD_VOLUME));
                        if (volumeData.isEmpty()) {
                            log.warn("Empty volume for externalVolumeEvent: {}. StoragePool: {}", event, volumeData);
                            return;
                        }

                        volumeData.put(ObjectMetaDataManager.ACCOUNT_FIELD, event.getAccountId());
                        volumeData.put(FIELD_ATTACHED_STATE, CommonStatesConstants.INACTIVE);
                        volumeData.put(FIELD_ALLOC_STATE, CommonStatesConstants.ACTIVE);
                        volumeData.put(FIELD_ZONE_ID, 1L);
                        volumeData.put(FIELD_DEV_NUM, -1);

                        try {
                            volumeDao.createVolumeInStoragePool(volumeData, name.toString(), storagePool);
                        } catch (ProcessCancelException e) {
                            log.info("Create process cancelled for volumeData {}. ProcessCancelException message: {}", volumeData, e.getMessage());
                        }
                    }
                    break;
                default:
                    log.error("Unknown event type: {} for event {}", event.getEventType(), event);
                    return;
                }
            }
        });
    }

    protected void handleStoragePoolEvent(final ExternalEvent event, ProcessState state, ProcessInstance process) {
        lockManager.lock(new ExternalEventLock(STORAGE_POOL_LOCK_NAME, event.getAccountId(), event.getExternalId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Object driver = CollectionUtils.getNestedValue(DataUtils.getFields(event), FIELD_STORAGE_POOL, FIELD_DRIVER_NAME);
                if (driver == null) {
                    log.warn("Driver not specified. Returning. Event: {}", ObjectUtils.toStringWrapper(event));
                    return;
                }
                String driverName = driver.toString();
                List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(event.getAccountId(), driverName);
                StoragePool storagePool = pools.size() > 0 ? pools.get(0) : null;
                Map<String, Object> spData = CollectionUtils.toMap(DataUtils.getFields(event).get(FIELD_STORAGE_POOL));
                if (spData.isEmpty()) {
                    log.warn("Null or empty storagePool for externalStoragePoolEvent: {}. StoragePool: {}", event, spData);
                    return;
                }

                if (storagePool == null) {
                    spData.put(ObjectMetaDataManager.ACCOUNT_FIELD, event.getAccountId());
                    spData.put(FIELD_ZONE_ID, 1L);

                    Agent agent = objectManager.findOne(Agent.class, AGENT.ACCOUNT_ID, event.getReportedAccountId(),
                            AGENT.STATE, CommonStatesConstants.ACTIVE);
                    spData.put(FIELD_AGENT_ID, agent.getId());

                    try {
                        storagePool = resourceDao.createAndSchedule(StoragePool.class, spData);
                    } catch (ProcessCancelException e) {
                        log.info("Create process cancelled for storagePool {}. ProcessCancelException message: {}", storagePool, e.getMessage());
                    }
                } else {
                    Agent agent = objectManager.findOne(Agent.class, AGENT.ACCOUNT_ID, event.getReportedAccountId(),
                            AGENT.STATE, CommonStatesConstants.ACTIVE);
                    spData.put(FIELD_AGENT_ID, agent.getId());

                    storagePool.setAgentId(agent.getId());
                    try {
                        storagePool = resourceDao.updateAndSchedule(storagePool);
                    } catch (ProcessCancelException e) {
                        log.info("Create process cancelled for storagePool {}. ProcessCancelException message: {}", storagePool, e.getMessage());
                    }
                }

                List<String> hostUuids = new ArrayList<String>();
                for (Object item : CollectionUtils.toList(DataUtils.getFields(event).get(FIELD_HOST_UUIDS))) {
                    if (item != null)
                        hostUuids.add(item.toString());
                }

                Map<Long, StoragePoolHostMap> maps = constructStoragePoolMaps(storagePool, hostUuids);
                try {
                    removeOldMaps(storagePool, maps);
                    createNewMaps(storagePool, maps);
                } catch (ProcessCancelException e) {
                    log.info("Process cancelled while syncing volumes to storagePool {}. ProcessCancelException message: {}", storagePool, e.getMessage());
                }
            }
        });
    }

    protected void handleExternalDnsEvent(final ExternalEvent event, ProcessState state, ProcessInstance process) {
        lockManager.lock(new ExternalEventLock(EXERNAL_DNS_LOCK_NAME, event.getAccountId(), event.getExternalId()),
                new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        String fqdn = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_FQDN);
                        String serviceName = DataAccessor
                                .fieldString(event, ExternalEventConstants.FIELD_SERVICE_NAME);
                        String stackName = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_STACK_NAME);
                        if (fqdn == null || serviceName == null || stackName == null) {
                            log.info("External DNS [event: " + event.getId() + "] misses some fields");
                            return;
                        }

                        Stack stack = objectManager.findAny(Stack.class, STACK.ACCOUNT_ID,
                                event.getAccountId(), STACK.REMOVED, null, STACK.NAME, stackName);
                        if (stack == null) {
                            log.info("Stack not found for external DNS [event: " + event.getId() + "]");
                            return;
                        }
                        Service service = objectManager.findAny(Service.class, SERVICE.ACCOUNT_ID,
                                event.getAccountId(), SERVICE.REMOVED, null, SERVICE.STACK_ID, stack.getId(),
                                SERVICE.NAME, serviceName);
                        if (service == null) {
                            log.info("Service not found for external DNS [event: " + event.getId() + "]");
                            return;
                        }
                        Map<String, Object> data = new HashMap<>();
                        data.put(ExternalEventConstants.FIELD_FQDN, fqdn);
                        DataUtils.getWritableFields(service).putAll(data);
                        objectManager.persist(service);
                        objectProcessManager.scheduleStandardProcessAsync(StandardProcess.UPDATE, service, data);
                    }
                });
    }

    protected void createNewMaps(StoragePool storagePool, Map<Long, StoragePoolHostMap> maps) {
        for (StoragePoolHostMap m : maps.values()) {
            storagePoolDao.createStoragePoolHostMap(m);
        }
    }

    protected void removeOldMaps(StoragePool storagePool, Map<Long, StoragePoolHostMap> newMaps) {
        List<? extends StoragePoolHostMap> existingMaps = storagePoolDao.findMapsToRemove(storagePool.getId());
        List<StoragePoolHostMap> toRemove = new ArrayList<StoragePoolHostMap>();

        for (StoragePoolHostMap m : existingMaps) {
            if (!newMaps.containsKey(m.getHostId())) {
                toRemove.add(m);
            }
        }

        for (StoragePoolHostMap m : toRemove) {
            StoragePoolHostMap remove = storagePoolDao.findNonremovedMap(m.getStoragePoolId(), m.getHostId());
            if (remove != null) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, remove, null);
            }
        }
    }

    protected Map<Long, StoragePoolHostMap> constructStoragePoolMaps(StoragePool storagePool, List<String> hostUuids) {
        List<? extends Host> hosts = hostDao.getHosts(storagePool.getAccountId(), hostUuids);
        Map<Long, StoragePoolHostMap> maps = new HashMap<Long, StoragePoolHostMap>();
        for (Host h : hosts) {
            StoragePoolHostMap sphm = objectManager.newRecord(StoragePoolHostMap.class);
            sphm.setHostId(h.getId());
            sphm.setStoragePoolId(storagePool.getId());
            maps.put(h.getId(), sphm);
        }
        return maps;
    }
}
