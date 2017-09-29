package io.cattle.platform.condition.deployment.impl;

import io.cattle.platform.condition.Condition;
import io.cattle.platform.condition.ConditionValues;
import io.cattle.platform.condition.deployment.ServiceDependency;
import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public class ServiceDependencyImpl implements ServiceDependency, ConditionValues<String> {

    Condition<String> condition;
    List<BiConsumer<String, Boolean>> setters = new ArrayList<>();

    public ServiceDependencyImpl(ExecutorService executorService) {
        this.condition = new Condition<>(executorService, this);
    }

    @Override
    public String getKey(String key) {
        return key;
    }

    @Override
    public void setValueSetters(BiConsumer<String, Boolean> valueSetters) {
        this.setters.add(valueSetters);
    }

    @Override
    public boolean loadInitialValue(String obj) {
        return false;
    }

    @Override
    public boolean satified(long accountId, long stackId, Long hostId, DependsOn dependsOn, Runnable callback) {
        String targetType = "container";
        String target = dependsOn.getContainer();
        if (StringUtils.isBlank(target)) {
            targetType = "service";
            target = dependsOn.getService();
        }

        if (StringUtils.isBlank(target)) {
            return true;
        }

        if (!target.contains("/")) {
            target = stackId + "/" + target;
        }

        String key;
        if (dependsOn.getCondition() == DependsOn.DependsOnCondition.healthylocal) {
            key = targetType + "/" + target + "/" + hostId;
        } else {
            key = targetType + "/" + target + "/" + dependsOn.getCondition();
        }

        return condition.check(key, callback);
    }

    @Override
    public void setState(StackInfo stack, ServiceInfo service, InstanceInfo instance) {
        if (stack == null) {
            return;
        }

        if (service == null && instance == null) {
            return;
        }

        setters.forEach(setter -> {
            if (service == null) {
                setContainer(setter, stack, instance);
            } else {
                setService(setter, stack, service, instance);
            }
        });
    }

    private void setContainer(BiConsumer<String, Boolean> setter, StackInfo stack, InstanceInfo instance) {
        boolean running = InstanceConstants.STATE_RUNNING.equals(instance.getState());
        boolean healthy = running && HealthcheckConstants.isHealthy(instance.getHealthState());

        setter.accept("container/" + stack.getId() + "/" + instance.getName() + "/running", running);
        setter.accept("container/" + stack.getName() + "/" + instance.getName() + "/running", running);
        setter.accept("container/" + stack.getId() + "/" + instance.getName() + "/healthy", healthy);
        setter.accept("container/" + stack.getName() + "/" + instance.getName() + "/healthy", healthy);
    }


    private void setService(BiConsumer<String, Boolean> setter, StackInfo stack, ServiceInfo service, InstanceInfo instance) {
        boolean running = CommonStatesConstants.ACTIVE.equals(service.getState());
        boolean healthy = running && isHealthy(service.getHealthState());

        setter.accept("service/" + stack.getId() + "/" + service.getName() + "/running", running);
        setter.accept("service/" + stack.getName() + "/" + service.getName() + "/running", running);
        setter.accept("service/" + stack.getId() + "/" + service.getName() + "/healthy", healthy);
        setter.accept("service/" + stack.getName() + "/" + service.getName() + "/healthy", healthy);

        if (service.isGlobal() && instance != null && instance.getHostId() != null) {
            running = InstanceConstants.STATE_RUNNING.equals(instance.getState());
            healthy = running && HealthcheckConstants.isHealthy(instance.getHealthState());
            setter.accept("service/" + stack.getId() + "/" + service.getName() + "/" + instance.getHostId(), healthy);
            setter.accept("service/" + stack.getName() + "/" + service.getName() + "/" + instance.getHostId(), healthy);
        }
    }

    private boolean isHealthy(String healthState) {
        if (HealthcheckConstants.HEALTH_STATE_DEGRADED.equals(healthState)) {
            return true;
        }
        return HealthcheckConstants.isHealthy(healthState);
    }

}
