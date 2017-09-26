package io.cattle.platform.condition.deployment.impl;

import io.cattle.platform.condition.Condition;
import io.cattle.platform.condition.ConditionValues;
import io.cattle.platform.condition.deployment.HealthyHosts;
import io.cattle.platform.core.model.DeploymentUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public class HealthyHostsImpl implements HealthyHosts, ConditionValues<HealthyHostsImpl.HostCluster> {

    Condition<HostCluster> condition;
    List<BiConsumer<String, Boolean>> setters = new ArrayList<>();

    public HealthyHostsImpl(ExecutorService executorService) {
        this.condition = new Condition<>(executorService, this);
    }

    @Override
    public boolean hostIsHealthy(DeploymentUnit unit, Runnable callback) {
        return condition.check(new HostCluster(unit), callback);
    }

    @Override
    public void setHostHealth(long hostId, boolean good) {
        this.setters.forEach(consumer -> consumer.accept("host/" + hostId, good));
    }

    @Override
    public void setClusterHealth(long clusterId, boolean good) {
        this.setters.forEach(consumer -> consumer.accept("cluster/" + clusterId, good));
    }

    @Override
    public String getKey(HostCluster obj) {
        if (obj.hostId == null) {
            return "cluster/" + obj.clusterId;
        } else {
            return "host/" + obj.hostId;
        }
    }

    @Override
    public void setValueSetters(BiConsumer<String, Boolean> valueSetters) {
        this.setters.add(valueSetters);
    }

    @Override
    public boolean loadInitialValue(HostCluster obj) {
        return false;
    }

    public static final class HostCluster {
        Long hostId;
        Long clusterId;

        public HostCluster(long hostId) {
            this.hostId = hostId;
        }

        public HostCluster(DeploymentUnit unit) {
            this.hostId = unit.getHostId();
            this.clusterId = unit.getClusterId();
        }
    }

}
