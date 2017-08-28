package io.cattle.platform.loop;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.cattle.platform.core.constants.HealthcheckConstants.*;
import static java.util.stream.Collectors.*;

public class HealthStateCalculateLoop implements Loop {

    long accountId;
    MetadataManager metadataManager;
    ObjectManager objectManager;

    public HealthStateCalculateLoop(long accountId, MetadataManager metadataManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.metadataManager = metadataManager;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(List<Object> input) {
        Metadata metadata = metadataManager.getMetadataForAccount(accountId);

        ListValuedMap<Long, String> serviceStates = calculateInstanceHealth(metadata);
        ListValuedMap<Long, String> stackStates = calculateServiceHealth(serviceStates, metadata);
        calculateStackHealth(stackStates, metadata);

        return Result.DONE;
    }

    private void calculateStackHealth(ListValuedMap<Long, String> stackStates, Metadata metadata) {
        for (StackInfo stackInfo : metadata.getStacks()) {
            String stackState = aggregate(stackStates.get(stackInfo.getId()));
            if (Objects.equals(stackState, stackInfo.getHealthState())) {
                writeStackHealthState(metadata, stackInfo.getId(), stackState);
            }
        }
    }

    private ListValuedMap<Long, String> calculateServiceHealth(ListValuedMap<Long, String> serviceStates, Metadata metadata) {
        ListValuedMap<Long, String> stackState = new ArrayListValuedHashMap<>();

        for (ServiceInfo serviceInfo : metadata.getServices()) {
            Long stackId = serviceInfo.getStackId();
            List<String> healthStates = serviceStates.get(serviceInfo.getId());
            if (healthStates == null) {
                healthStates = new ArrayList<>();
            }

            // Haven't met the scale yet
            if (!serviceInfo.isGlobal() && serviceInfo.getScale() != null &&
                    healthStates.size() != (serviceInfo.getScale() * (1+serviceInfo.getSidekicks().size()))) {
                healthStates.add(HEALTH_STATE_DEGRADED);
            }

            String serviceState = aggregate(healthStates);
            if (!Objects.equals(serviceState, serviceInfo.getHealthState())) {
                writeServiceHealthState(metadata, serviceInfo.getId(), serviceState);
            }

            stackState.put(stackId, serviceState);
        }

        return stackState;
    }

    private ListValuedMap<Long, String> calculateInstanceHealth(Metadata metadata) {
        ListValuedMap<Long, String> serviceState = new ArrayListValuedHashMap<>();

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            String instanceState = instanceInfo.getHealthState();

            if (InstanceConstants.STATE_RUNNING.equals(instanceInfo.getState())) {
                instanceState = aggregate(healthStates(instanceInfo));
            } else if (InstanceConstants.STATE_STOPPED.equalsIgnoreCase(instanceInfo.getState())) {
                if (instanceInfo.isShouldRestart()) {
                    instanceState = HEALTH_STATE_UNHEALTHY;
                } else if (instanceInfo.getExitCode() == null || instanceInfo.getExitCode() == 0) {
                    instanceState = HEALTH_STATE_HEALTHY;
                } else {
                    instanceState = HEALTH_STATE_UNHEALTHY;
                }
            }

            Long serviceId = instanceInfo.getServiceId();
            if (serviceId != null) {
                serviceState.put(serviceId, instanceState);
            }

            if (!Objects.equals(instanceState, instanceInfo.getHealthState())) {
                writeInstanceHealthState(metadata, instanceInfo.getId(), instanceState);
            }
        }

        return serviceState;
    }

    private List<String> healthStates(InstanceInfo instanceInfo) {
        return instanceInfo.getHealthCheckHosts().stream()
                .map(HealthcheckState::getHealthState)
                .collect(toList());
    }

    private String aggregate(List<String> states) {
        if (states == null) {
            return null;
        }

        String result = null;
        boolean allSame = true;
        Set<String> statesSeen = new HashSet<>();

        for (String next : states) {
            if (result == null) {
                result = next;
            }
            statesSeen.add(next);

            if (Objects.equals(result, next)) {
                continue;
            }
            allSame = false;
        }

        if (allSame) {
            // If all are the same that is the state
            return result;
        }

        /* At this point, it can only be degraded or initializing as (un)healthy requires that all states agree */
        if (statesSeen.contains(HEALTH_STATE_UNHEALTHY) || statesSeen.contains(HEALTH_STATE_DEGRADED)) {
            return HEALTH_STATE_DEGRADED;
        } else if (statesSeen.contains(HEALTH_STATE_INITIALIZING)) {
            return HEALTH_STATE_INITIALIZING;
        }

        return null;
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
