package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.HEALTHCHECK_INSTANCE;
import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.ArrayList;
import java.util.Collections;
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
    NetworkDao networkDao;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public void updateHealthcheck(HealthcheckInstanceType instanceType, long instanceId, boolean healthy) {
        if (instanceType == HealthcheckInstanceType.INSTANCE) {
            Instance instance = objectManager.loadResource(Instance.class, instanceId);
            if (instance != null) {
                if (healthy) {
                    objectProcessManager.scheduleProcessInstance("instance.updatehealthy", instance, null);
                } else {
                    objectProcessManager.scheduleProcessInstance("instance.updateunhealthy", instance, null);
                }
            }
        }
    }

    @Override
    public void registerForHealtcheck(HealthcheckInstanceType instanceType, long instanceId) {
        Long accountId = getAccountId(instanceType, instanceId);
        if (accountId == null) {
            return;
        }
        
        // 1. create healthcheckInstance mapping
        HealthcheckInstance healthInstance = objectManager.findAny(HealthcheckInstance.class,
                HEALTHCHECK_INSTANCE.ACCOUNT_ID, accountId, HEALTHCHECK_INSTANCE.KIND, instanceType,
                HEALTHCHECK_INSTANCE.INSTANCE_ID, instanceId,
                HEALTHCHECK_INSTANCE.REMOVED, null);
        
        if (healthInstance == null) {
            healthInstance = resourceDao.createAndSchedule(HealthcheckInstance.class,
                    HEALTHCHECK_INSTANCE.ACCOUNT_ID, accountId, HEALTHCHECK_INSTANCE.KIND, instanceType,
                    HEALTHCHECK_INSTANCE.INSTANCE_ID, instanceId);
        }
        
        // 2. create healtcheckInstance to hosts mappings
        Long inferiorHostId = getInstanceHostId(instanceType, instanceId);
        List<Long> healthCheckHostIds = getHealthCheckHostIds(healthInstance, inferiorHostId);
        for (Long healthCheckHostId : healthCheckHostIds) {
            HealthcheckInstanceHostMap healthHostMap = mapDao.findNonRemoved(HealthcheckInstanceHostMap.class,
                    Host.class, healthCheckHostId, HealthcheckInstance.class,
                    healthInstance.getId());
            if (healthHostMap == null) {
                resourceDao.createAndSchedule(HealthcheckInstanceHostMap.class, HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID,
                        healthCheckHostId,
                        HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID, healthInstance.getId(),
                        HEALTHCHECK_INSTANCE_HOST_MAP.ACCOUNT_ID,
                        accountId);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private List<Long> getHealthCheckHostIds(HealthcheckInstance healthInstance, Long inferiorHostId) {
        int requiredNumber = 3;

        // 1) check if have to allocate more hosts
        List<? extends HealthcheckInstanceHostMap> existingHostMaps = mapDao.findNonRemoved(
                HealthcheckInstanceHostMap.class,
                HealthcheckInstance.class, healthInstance.getId());

        if (existingHostMaps.size() >= requiredNumber) {
            return new ArrayList<>();
        }

        // 2) Figure out what hosts to allocate
        List<Host> availableHosts = objectManager.find(Host.class, HOST.ACCOUNT_ID, healthInstance.getAccountId(),
                HOST.STATE, CommonStatesConstants.ACTIVE, HOST.REMOVED, null);
        List<Long> availableHostIds = (List<Long>) CollectionUtils.collect(availableHosts,
                TransformerUtils.invokerTransformer("getId"));
        List<Long> allocatedHostIds = (List<Long>) CollectionUtils.collect(existingHostMaps,
                TransformerUtils.invokerTransformer("getHostId"));

        // 3) remove allocated hosts from the available hostIds and shuffle the list to randomize the choice
        availableHostIds.removeAll(allocatedHostIds);
        Collections.shuffle(availableHostIds);

        // 4) place inferiorHostId to the end of the list
        if (inferiorHostId != null) {
            availableHostIds.remove(inferiorHostId);
            availableHostIds.add(inferiorHostId);
        }

        // 5) Figure out the final number of hosts
        int returnedNumber = requiredNumber > availableHostIds.size() ? availableHostIds.size() : requiredNumber;

        return availableHostIds.subList(0, returnedNumber);
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
