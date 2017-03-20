package io.cattle.platform.servicediscovery.deployment;


public interface DeploymentUnit {

    String getStatus();

    void stop();

    void remove(String reason, String level);

    void deploy();

    boolean needToReconcile();

    boolean isUnhealthy();

}
