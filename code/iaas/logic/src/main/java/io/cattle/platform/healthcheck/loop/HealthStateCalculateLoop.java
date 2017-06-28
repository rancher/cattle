package io.cattle.platform.healthcheck.loop;

import static io.cattle.platform.core.constants.HealthcheckConstants.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.model.StackInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;

public class HealthStateCalculateLoop implements Loop {

    long accountId;
    EnvironmentResourceManager envResourceManager;
    ObjectManager objectManager;

    public HealthStateCalculateLoop(long accountId, EnvironmentResourceManager envResourceManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.envResourceManager = envResourceManager;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(Object input) {
        Metadata metadata = envResourceManager.getMetadata(accountId);

        Map<Long, String> serviceStates = calculateServiceHealth(metadata);
        Map<Long, String> stackStates = calculateStackHealth(serviceStates, metadata);
        calculateEnvHealth(stackStates, metadata);

        return Result.DONE;
    }

    private void calculateEnvHealth(Map<Long, String> stackStates, Metadata metadata) {
        for (StackInfo stackInfo : metadata.getStacks()) {
            String stackState = stackStates.get(stackInfo.getId());
            if (stackState == null) {
                stackState = HEALTH_STATE_HEALTHY;
            }

            if (!stackState.equals(stackInfo.getHealthState())) {
                writeStackHealthState(metadata, stackInfo.getId(), stackState);
            }
        }
    }

    private Map<Long, String> calculateStackHealth(Map<Long, String> serviceStates, Metadata metadata) {
        Map<Long, String> stackState = new HashMap<>();

        for (ServiceInfo serviceInfo : metadata.getServices()) {
            Long stackId = serviceInfo.getStackId();
            if (stackId == null) {
                continue;
            }

            String serviceState = serviceStates.get(serviceInfo.getId());
            if (serviceState == null) {
                serviceState = HEALTH_STATE_HEALTHY;
            }

            if (!serviceState.equals(serviceInfo.getHealthState())) {
                writeServiceHealthState(metadata, serviceInfo.getId(), serviceState);
            }

            stackState.put(stackId, aggregate(stackState.get(stackId), serviceState));
        }

        return stackState;
    }

    private Map<Long, String> calculateServiceHealth(Metadata metadata) {
        Map<Long, String> serviceState = new HashMap<>();

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            Long serviceId = instanceInfo.getServiceId();
            if (serviceId == null) {
                continue;
            }

            String instanceState = null;
            if (instanceInfo.getHealthCheck() == null) {
                instanceState = HEALTH_STATE_HEALTHY;
            } else if (InstanceConstants.STATE_RUNNING.equals(instanceInfo.getState())) {
                // Health state is based on checkers when runnning
                instanceState = aggregate(instanceInfo.getHealthCheckHosts().stream().map((x) -> x.getHealthState()).toArray((i) -> new String[i]));
            } else {
                // Else use the save state, which should have been updated on stop
                instanceState = instanceInfo.getHealthState();
            }

            if (instanceState == null) {
                instanceState = HEALTH_STATE_HEALTHY;
            }

            if (!instanceState.equals(instanceInfo.getHealthState())) {
                writeInstanceHealthState(metadata, instanceInfo.getId(), instanceState);
            }

            serviceState.put(serviceId, aggregate(serviceState.get(serviceId), instanceState));
        }

        return serviceState;
    }

    private String aggregate(String... states) {
        String result = HEALTH_STATE_HEALTHY;

        for (String next : states) {
            if (next == null) {
                continue;
            }

            if (priority(result) < priority(next)) {
                result = next;
            }
        }

        return result;
    }

    private int priority(String state) {
        switch (state) {
        case HEALTH_STATE_UNHEALTHY:
            return 4;
        case SERVICE_HEALTH_STATE_DEGRADED:
            return 3;
        case HEALTH_STATE_INITIALIZING:
            return 2;
        case HEALTH_STATE_HEALTHY:
            return 1;
        default:
            return 0;
        }
    }

    private void writeInstanceHealthState(Metadata metadata, long id, String healthState) {
        metadata.modify(Instance.class, id, (instance) -> {
            instance.setHealthState(healthState);
            return objectManager.persist(instance);
        });
    }

    private void writeServiceHealthState(Metadata metadata, long id, String healthState) {
        metadata.modify(Service.class, id, (service) -> {
            service.setHealthState(healthState);
            return objectManager.persist(service);
        });
    }

    private void writeStackHealthState(Metadata metadata, long id, String healthState) {
        metadata.modify(Stack.class, id, (stack) -> {
            stack.setHealthState(healthState);
            return objectManager.persist(stack);
        });
    }
}
