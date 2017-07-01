package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.metadata.model.HealthcheckInfo;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.netflix.config.DynamicIntProperty;

public class HealthcheckCleanupMonitorImpl implements Loop {

    private static final DynamicIntProperty DEFAULT_TIMEOUT = ArchaiusUtil.getInt("healthcheck.default.initializing.timeout");

    long accountId;
    ObjectManager objectManager;
    LoopManager loopManager;
    ScheduledExecutorService scheduledExecutorService;
    EnvironmentResourceManager envResourceManager;
    Map<Long, Long> firstSeen = new HashMap<>();

    public HealthcheckCleanupMonitorImpl(long accountId, ObjectManager objectManager, LoopManager loopManager,
            ScheduledExecutorService scheduledExecutorService, EnvironmentResourceManager envResourceManager) {
        super();
        this.accountId = accountId;
        this.objectManager = objectManager;
        this.loopManager = loopManager;
        this.scheduledExecutorService = scheduledExecutorService;
        this.envResourceManager = envResourceManager;
    }

    @Override
    public Result run(Object input) {
        Metadata metadata = envResourceManager.getMetadata(accountId);
        Map<Long, Long> firstSeen = new HashMap<>();
        Long checkNext = null;

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            if (instanceInfo.getHealthCheck() == null) {
                continue;
            }

            if (!InstanceConstants.STATE_RUNNING.equals(instanceInfo.getState())) {
                continue;
            }

            if (!HealthcheckConstants.HEALTH_STATE_INITIALIZING.equals(instanceInfo.getHealthState())) {
                continue;
            }

            Long delay = whenToDelete(instanceInfo, firstSeen);
            if (delay <= 0) {
                Instance instance = metadata.modify(Instance.class, instanceInfo.getId(), (i) -> {
                    i.setHealthState(HealthcheckConstants.HEALTH_STATE_UNHEALTHY);
                    return objectManager.persist(i);
                });
                if (instance.getDeploymentUnitId() != null) {
                    loopManager.kick(LoopFactory.DU_RECONCILE, DeploymentUnit.class, instance.getDeploymentUnitId(), input);
                }
            } else if (checkNext == null) {
                checkNext = delay;
            } else if (delay < checkNext) {
                checkNext = delay;
            }
        }

        this.firstSeen = firstSeen;
        if (checkNext != null) {
            scheduledExecutorService.schedule(
                    () -> loopManager.kick(LoopFactory.HEALTHCHECK_CLEANUP, Account.class, accountId, null),
                    checkNext,
                    TimeUnit.MILLISECONDS);
            return Result.WAITING;
        }

        return Result.DONE;
    }

    protected Long whenToDelete(InstanceInfo instanceInfo, Map<Long, Long> firstSeen) {
        Long start = this.firstSeen.get(instanceInfo.getId());
        if (start == null) {
            start = System.currentTimeMillis();
        }

        HealthcheckInfo healthCheck = instanceInfo.getHealthCheck();
        Integer timeout = healthCheck.getInitializingTimeout();
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT.get();
        }

        Long result = timeout - (System.currentTimeMillis() - start);
        if (result > 0) {
            firstSeen.put(instanceInfo.getId(), start);
        }

        return result;
    }

}
