package io.cattle.platform.servicediscovery.deployment;

public interface DeploymentUnitInstanceIdGenerator {

    Integer getNextAvailableId(String launchConfigName);
}
