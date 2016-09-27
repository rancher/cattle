package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.constants.HealthcheckConstants.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class HealthcheckServiceImpl implements HealthcheckService {
    
    @Inject
    GenericMapDao mapDao;
    
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    LockManager lockManager;

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    HostDao hostDao;

    @Inject
    NetworkDao ntwkDao;

    @Override
    public void updateHealthcheck(String healthcheckInstanceHostMapUuid, final long externalTimestamp,
            final String healthState) {
        HealthcheckInstanceHostMap hcihm = objectManager.findOne(HealthcheckInstanceHostMap.class,
                ObjectMetaDataManager.UUID_FIELD, healthcheckInstanceHostMapUuid);

        if (!shouldUpdate(hcihm, externalTimestamp, healthState)) {
            return;
        }

        String hcihmNewState = healthState;
        if (healthState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_INITIALIZING)) {
            if (!hcihm.getState().equalsIgnoreCase(healthState)) {
                hcihmNewState = HealthcheckConstants.HEALTH_STATE_REINITIALIZING;
            }
        }
        
        final HealthcheckInstanceHostMap updatedHcihm = objectManager.setFields(hcihm,
                HEALTHCHECK_INSTANCE_HOST_MAP.EXTERNAL_TIMESTAMP, externalTimestamp,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTH_STATE, hcihmNewState);

        lockManager.lock(new HealthcheckInstanceLock(hcihm.getHealthcheckInstanceId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                processHealthcheckInstance(updatedHcihm, healthState);
            }
        });
    }

    @Override
    public void healthCheckReconcile(final HealthcheckInstanceHostMap hcihm, final String healthState) {
        final HealthcheckInstance hcInstance = objectManager.loadResource(HealthcheckInstance.class,
                hcihm.getHealthcheckInstanceId());
        lockManager.lock(new HealthcheckInstanceLock(hcInstance.getId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                processHealthcheckInstance(hcihm, healthState);
            }
        });
    }

    protected void processHealthcheckInstance(HealthcheckInstanceHostMap hcihm, String healthState) {
        HealthcheckInstance hcInstance = objectManager.loadResource(HealthcheckInstance.class, hcihm.getHealthcheckInstanceId());
        String updateWithState = determineNewHealthState(hcInstance, hcihm, healthState);
        if (updateWithState == null) {
            return;
        }
        Instance instance = objectManager.loadResource(Instance.class, hcInstance.getInstanceId());
        updateInstanceHealthState(instance, updateWithState);
    }


    protected boolean shouldUpdate(HealthcheckInstanceHostMap hcihm, long externalTimestamp, String healthState) {
        HealthcheckInstance hcInstance = objectManager.loadResource(HealthcheckInstance.class,
                hcihm.getHealthcheckInstanceId());
        if (healthState.equalsIgnoreCase(HEALTH_STATE_UNHEALTHY)
                && HealthcheckConstants.isInit(getHealthState(hcInstance))) {
            return false;
        }

        if (hcihm.getExternalTimestamp() == null) {
            return true;
        }

        if (externalTimestamp < hcihm.getExternalTimestamp()) {
            return false;
        }

        return true;
    }

    protected String determineNewHealthState(HealthcheckInstance hcInstance, HealthcheckInstanceHostMap hcihm,
            String healthState) {
        List<HealthcheckInstanceHostMap> others = objectManager.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID, hcInstance.getId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.STATE, CommonStatesConstants.ACTIVE);

        boolean currentlyHealthy = HEALTH_STATE_HEALTHY.equals(getHealthState(hcInstance));

        if (healthState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_HEALTHY)) {
            return currentlyHealthy ? null : healthState;
        } else if (healthState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_INITIALIZING)) {
            return currentlyHealthy ? HealthcheckConstants.HEALTH_STATE_REINITIALIZING : null;
        } else {
            if (!currentlyHealthy) {
                return null;
            }

            boolean allUnHealthy = true;
            int i = 0;
            for (HealthcheckInstanceHostMap map : others) {
                if (map.getId().equals(hcihm.getId())) {
                    continue;
                }
                i++;
                if (!HEALTH_STATE_UNHEALTHY.equals(map.getHealthState())) {
                    allUnHealthy = false;
                    break;
                }
            }

            if (healthState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_RECONCILE)) {
                // if no other hosts are present, don't change the state;
                // otherwise calculate based on the health check present
                if (i == 0) {
                    return null;
                } else {
                    return allUnHealthy ? HealthcheckConstants.HEALTH_STATE_UNHEALTHY
                            : null;
                }
            }

            return allUnHealthy ? healthState : null;
        }
    }

    protected String getHealthState(HealthcheckInstance hcInstance) {
        Instance instance = objectManager.loadResource(Instance.class, hcInstance.getInstanceId());

        return instance == null ? null : instance.getHealthState();
    }

    @Override
    public void updateInstanceHealthState(Instance instance, String updateWithState) {
        if (instance != null) {
            if (updateWithState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_HEALTHY)) {
                objectProcessManager.scheduleProcessInstance(HealthcheckConstants.PROCESS_UPDATE_HEALTHY, instance,
                        null);
            } else if (updateWithState.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_REINITIALIZING)) {
                objectProcessManager.scheduleProcessInstance(HealthcheckConstants.PROCESS_UPDATE_REINITIALIZING,
                        instance, null);
            } else {
                objectProcessManager.scheduleProcessInstance(HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY, instance,
                        null);
            }
        }
    }

    @Override
    public void registerForHealtcheck(final HealthcheckInstanceType instanceType, final long id) {
        final Long accountId = getAccountId(instanceType, id);
        if (accountId == null) {
            return;
        }
        
        lockManager.lock(new HealthcheckRegisterLock(id, instanceType), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // 1. create healthcheckInstance mapping
                HealthcheckInstance healthInstance = createHealtcheckInstance(id, accountId);

                // 2. create healtcheckInstance to hosts mappings
                createHealthCheckHostMaps(instanceType, id, accountId, healthInstance);
            }
        });
    }

    protected HealthcheckInstance createHealtcheckInstance(long id, Long accountId) {
        HealthcheckInstance healthInstance = objectManager.findAny(HealthcheckInstance.class,
                HEALTHCHECK_INSTANCE.ACCOUNT_ID, accountId,
                HEALTHCHECK_INSTANCE.INSTANCE_ID, id,
                HEALTHCHECK_INSTANCE.REMOVED, null);
        
        if (healthInstance == null) {
            healthInstance = resourceDao.createAndSchedule(HealthcheckInstance.class,
                    HEALTHCHECK_INSTANCE.ACCOUNT_ID, accountId,
                    HEALTHCHECK_INSTANCE.INSTANCE_ID, id);
        }
        return healthInstance;
    }

    protected void createHealthCheckHostMaps(HealthcheckInstanceType instanceType, long id, Long accountId,
            HealthcheckInstance healthInstance) {
        Long inferiorHostId = getInstanceHostId(instanceType, id);
        List<Long> healthCheckHostIds = getHealthCheckHostIds(healthInstance, inferiorHostId);
        for (Long healthCheckHostId : healthCheckHostIds) {
            HealthcheckInstanceHostMap healthHostMap = mapDao.findNonRemoved(HealthcheckInstanceHostMap.class,
                    Host.class, healthCheckHostId, HealthcheckInstance.class,
                    healthInstance.getId());
            Long instanceId = (instanceType == HealthcheckInstanceType.INSTANCE ? id : null);
            if (healthHostMap == null) {
                resourceDao.createAndSchedule(HealthcheckInstanceHostMap.class, HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID,
                        healthCheckHostId,
                        HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID, healthInstance.getId(),
                        HEALTHCHECK_INSTANCE_HOST_MAP.ACCOUNT_ID,
                        accountId,
                        HEALTHCHECK_INSTANCE_HOST_MAP.INSTANCE_ID, instanceId);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private List<Long> getHealthCheckHostIds(HealthcheckInstance healthInstance, Long inferiorHostId) {
        int requiredNumber = 3;

        List<? extends HealthcheckInstanceHostMap> existingHostMaps = mapDao.findNonRemoved(
                HealthcheckInstanceHostMap.class,
                HealthcheckInstance.class, healthInstance.getId());
        List<? extends Host> availableActiveHosts = allocatorDao.getActiveHosts(healthInstance.getAccountId());
        
        List<Long> availableActiveHostIds = (List<Long>) CollectionUtils.collect(availableActiveHosts,
                TransformerUtils.invokerTransformer("getId"));
        List<Long> allocatedActiveHostIds = (List<Long>) CollectionUtils.collect(existingHostMaps,
                TransformerUtils.invokerTransformer("getHostId"));

        // skip the host that if not active (being removed, reconnecting, etc)
        Iterator<Long> it = allocatedActiveHostIds.iterator();
        while (it.hasNext()) {
            Long allocatedHostId = it.next();
            if (!availableActiveHostIds.contains(allocatedHostId)) {
                it.remove();
            }
        }

        // return if allocated active hosts is >= required number of hosts for healtcheck
        if (allocatedActiveHostIds.size() >= requiredNumber) {
            return new ArrayList<>();
        }

        // remove allocated hosts from the available hostIds and shuffle the list to randomize the choice
        availableActiveHostIds.removeAll(allocatedActiveHostIds);
        requiredNumber = requiredNumber - allocatedActiveHostIds.size();
        Collections.shuffle(availableActiveHostIds);

        // place inferiorHostId to the end of the list
        if (inferiorHostId != null) {
            if (availableActiveHostIds.contains(inferiorHostId)) {
                availableActiveHostIds.remove(inferiorHostId);
                if (availableActiveHostIds.isEmpty() && allocatedActiveHostIds.isEmpty()) {
                    availableActiveHostIds.add(inferiorHostId);
                }
            }
        }

        // Figure out the final number of hosts
        int returnedNumber = requiredNumber > availableActiveHostIds.size() ? availableActiveHostIds.size() : requiredNumber;

        return availableActiveHostIds.subList(0, returnedNumber);
    }

    private Long getInstanceHostId(HealthcheckInstanceType type, long instanceId) {
        if (type == HealthcheckInstanceType.INSTANCE) {
            List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class,
                    instanceId);
            if (!maps.isEmpty()) {
                return maps.get(0).getHostId();
            }
        }
        return null;
    }

    private Long getAccountId(HealthcheckInstanceType type, long instanceId) {
        if (type == HealthcheckInstanceType.INSTANCE) {
            Instance instance = objectManager.loadResource(Instance.class, instanceId);
            if (instance != null) {
                return instance.getAccountId();
            }
        }
        return null;
    }
}
