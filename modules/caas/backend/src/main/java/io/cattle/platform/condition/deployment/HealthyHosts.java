package io.cattle.platform.condition.deployment;

import io.cattle.platform.core.model.DeploymentUnit;

public interface HealthyHosts {

    boolean hostIsHealthy(DeploymentUnit unit, Runnable callback);

    void setHostHealth(long hostId, boolean good);

    void setClusterHealth(long clusterId, boolean good);

}
