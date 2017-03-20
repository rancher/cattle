package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Instance;

import java.util.Map;

public interface DeploymentUnitInstance {

    boolean isUnhealthy();

    void stop();

    void scheduleCreate();

    void remove(String reason, String level);

    String getLaunchConfigName();

    Instance getInstance();

    void create(Map<String, Object> deployParams);

    void waitForStart(boolean isDependee);

    DeploymentUnitInstance start(boolean isDependee);

    boolean isStarted(boolean isDependee);

    void waitForHealthy();

    boolean isHealthy();
    
    void waitForStop();

    void resetUpgrade(boolean upgrade);
    
    boolean isSetForUpgrade();

}
