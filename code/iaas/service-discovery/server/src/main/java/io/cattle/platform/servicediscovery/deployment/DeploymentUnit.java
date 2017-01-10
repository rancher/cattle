package io.cattle.platform.servicediscovery.deployment;


public interface DeploymentUnit {

    boolean isHealthCheckInitializing();

    boolean isUnhealthy();

    String getStatus();

    void stop();

    void remove(String reason, String level);

    void deploy();

    boolean needToReconcile();

}
