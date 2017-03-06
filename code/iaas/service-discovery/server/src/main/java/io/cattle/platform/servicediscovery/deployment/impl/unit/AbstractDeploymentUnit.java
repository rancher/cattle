package io.cattle.platform.servicediscovery.deployment.impl.unit;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDeploymentUnit implements io.cattle.platform.servicediscovery.deployment.DeploymentUnit {
    DeploymentUnitManagerContext context;
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    DeploymentUnit unit;
    String uuid;

    public AbstractDeploymentUnit(DeploymentUnitManagerContext context, DeploymentUnit unit) {
        this.context = context;
        this.uuid = unit.getUuid();
        this.unit = unit;
    }
    
    protected abstract void createImpl();

    @Override
    public abstract void remove(String reason, String level);

    @Override
    public abstract void cleanup(String reason, String level);

    protected abstract void cleanupDependencies();

    protected abstract void cleanupUnhealthy();

    protected abstract List<String> getSidekickRefs(String launchConfigName);
    
    protected abstract List<DeploymentUnitInstance> getInstancesWithMistmatchedIndexes();

    protected void sortSidekicks(List<String> sorted, String lc) {
        List<String> sidekicks = getSidekickRefs(lc);
        for (String sidekick : sidekicks) {
            sortSidekicks(sorted, sidekick);
        }
        if (!sorted.contains(lc)) {
            sorted.add(lc);
        }
    }

    @Override
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

    @Override
    public boolean isUnhealthy() {
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }
        return false;
    }

    @Override
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

    @Override
    public boolean isHealthCheckInitializing() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isHealthCheckInitializing()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isStarted() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    protected boolean isComplete() {
        return launchConfigToInstance.keySet().containsAll(launchConfigNames);
    }

    @Override
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

    @Override
    public boolean needToReconcile() {
        return isUnhealthy() || !isComplete() || !isStarted()
                || getInstancesWithMistmatchedIndexes().size() > 0;
    }
}
