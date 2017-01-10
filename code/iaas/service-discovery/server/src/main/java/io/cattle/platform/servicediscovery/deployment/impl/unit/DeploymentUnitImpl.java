package io.cattle.platform.servicediscovery.deployment.impl.unit;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DeploymentUnitImpl {
    DeploymentUnitManagerContext context;
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    DeploymentUnit unit;
    String uuid;

    public DeploymentUnitImpl(DeploymentUnitManagerContext context, DeploymentUnit unit) {
        this.context = context;
        this.uuid = unit.getUuid();
        this.unit = unit;
    }
    
    protected abstract void createImpl();

    public abstract void remove(String reason, String level);

    public abstract void cleanup(String reason, String level);

    protected abstract void cleanupDependencies();

    protected abstract void cleanupUnhealthy();

    protected abstract List<String> getSidekickRefs(String launchConfigName);
    
    public abstract List<DeploymentUnitInstance> getInstancesWithMistmatchedIndexes();

    protected void sortSidekicks(List<String> sorted, String lc) {
        List<String> sidekicks = getSidekickRefs(lc);
        for (String sidekick : sidekicks) {
            sortSidekicks(sorted, sidekick);
        }
        if (!sorted.contains(lc)) {
            sorted.add(lc);
        }
    }

    public void deploy() {
        cleanupInstancesWithMistmatchedIndexes();
        cleanupUnhealthy();
        cleanupDependencies();
        create();
        start();
        waitForStart();
        updateHealthy();
    }

    public void updateHealthy() {
        if (this.isUnhealthy()) {
            return;
        }
        if (HealthcheckConstants.isHealthy(this.unit.getHealthState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_DU_UPDATE_HEALTHY,
                    this.unit, null);
        }
    }

    public DeploymentUnit getUnit() {
        return unit;
    }

    public boolean isUnhealthy() {
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        /*
         * stops all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.stop();
        }
    }

    public void start() {
        for (DeploymentUnitInstance instance : getSortedDeploymentUnitInstances()) {
            instance.start();
        }
    }
    
    protected void create() {
        createImpl();
        for (DeploymentUnitInstance instance : launchConfigToInstance.values()) {
            instance.scheduleCreate();
        }
    }

    protected void waitForStart() {
        // sort based on dependencies
        List<DeploymentUnitInstance> sortedInstances = getSortedDeploymentUnitInstances();
        for (DeploymentUnitInstance instance : sortedInstances) {
            instance.waitForStart();
        }
    }

    public List<DeploymentUnitInstance> getSortedDeploymentUnitInstances() {
        List<String> sortedLCs = new ArrayList<>();
        for (String lc : launchConfigToInstance.keySet()) {
            sortSidekicks(sortedLCs, lc);
        }

        List<DeploymentUnitInstance> sortedInstances = new ArrayList<>();
        for (String lc : sortedLCs) {
            sortedInstances.add(launchConfigToInstance.get(lc));
        }
        Collections.reverse(sortedInstances);
        return sortedInstances;
    }
    
    public List<DeploymentUnitInstance> getDeploymentUnitInstances() {
        List<DeploymentUnitInstance> instances = new ArrayList<>();
        instances.addAll(launchConfigToInstance.values());
        return instances;
    }

    protected void addDeploymentInstance(String launchConfig, DeploymentUnitInstance instance) {
        this.launchConfigToInstance.put(launchConfig, instance);
    }

    protected void removeDeploymentUnitInstance(DeploymentUnitInstance instance, String reason, String level) {
        instance.remove(reason, level);
        launchConfigToInstance.remove(instance.getLaunchConfigName());
    }

    protected String getUuid() {
        return this.uuid;
    }

    public boolean isHealthCheckInitializing() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isHealthCheckInitializing()) {
                return true;
            }
        }
        return false;
    }

    public boolean isStarted() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    public boolean isComplete() {
        return launchConfigToInstance.keySet().containsAll(launchConfigNames);
    }

    public String getStatus() {
        return String.format("Healthy: %s, Complete: %s, Started: %s",
                !isUnhealthy(),
                isComplete(),
                isStarted());
    }

    protected void cleanupInstancesWithMistmatchedIndexes() {
        List<DeploymentUnitInstance> toCleanup = getInstancesWithMistmatchedIndexes();
        for (DeploymentUnitInstance i : toCleanup) {
            removeDeploymentUnitInstance(i, ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.INFO);
        }
    }
}
