package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Instance;

import java.util.Map;

public interface DeploymentUnitInstance {

    boolean isUnhealthy();

    void stop();

    void scheduleCreate();

    void remove(String reason, String level);

    boolean isHealthCheckInitializing();

    String getLaunchConfigName();

    Instance getInstance();

    void create(Map<String, Object> deployParams);

    DeploymentUnitInstance waitForStart(boolean isDependee);

    DeploymentUnitInstance start(boolean isDependee);

    boolean isStarted(boolean isDependee);
}
