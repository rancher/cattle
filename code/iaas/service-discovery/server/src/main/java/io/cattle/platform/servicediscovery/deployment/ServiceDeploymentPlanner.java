package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.DeploymentUnit;

import java.util.List;

public interface ServiceDeploymentPlanner {

    String getStatus();

    boolean isHealthcheckInitiailizing();

    List<DeploymentUnit> deploy();

    void deactivateUnits();

    void removeUnits();

    boolean needToReconcile();

}
