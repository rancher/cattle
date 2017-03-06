package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Instance;

import java.util.Map;

public interface DeploymentUnitInstance {

    boolean isUnhealthy();

    void stop();

    DeploymentUnitInstance start();

    void scheduleCreate();

    DeploymentUnitInstance waitForStart();

    void remove(String reason, String level);

    boolean isHealthCheckInitializing();

    String getLaunchConfigName();

    boolean isStarted();

    Instance getInstance();

    void create(Map<String, Object> deployParams);
}
