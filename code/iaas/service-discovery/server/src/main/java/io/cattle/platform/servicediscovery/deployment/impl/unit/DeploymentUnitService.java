package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.StackTable.STACK;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentUnitService {
    Service service;
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    DeploymentServiceContext context;
    Stack env;

    public DeploymentUnitService(Service service, List<String> launchConfigNames, DeploymentServiceContext context) {
        this.service = service;
        this.env = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());
        this.launchConfigNames = launchConfigNames;
        this.context = context;
        for (String launchConfigName : launchConfigNames) {
            for (String sidekick : getSidekickRefs(service, launchConfigName)) {
                List<String> usedBy = sidekickUsedByMap.get(sidekick);
                if (usedBy == null) {
                    usedBy = new ArrayList<>();
                }
                usedBy.add(launchConfigName);
                sidekickUsedByMap.put(sidekick, usedBy);
            }
        }
    }

    public void addDeploymentInstance(String launchConfig, DeploymentUnitInstance instance) {
        this.launchConfigToInstance.put(launchConfig, instance);
    }

    public List<DeploymentUnitInstance> getInstances() {
        List<DeploymentUnitInstance> instances = new ArrayList<>();
        instances.addAll(launchConfigToInstance.values());
        return instances;
    }

    public DeploymentUnitInstance getInstance(String launchConfigName) {
        return launchConfigToInstance.get(launchConfigName);
    }

    public Map<String, DeploymentUnitInstance> getLaunchConfigToInstance() {
        return launchConfigToInstance;
    }

    public boolean isComplete() {
        return launchConfigNames.size() == launchConfigToInstance.size();
    }

    public void createMissingInstances(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator, String uuid) {
        Integer order = null;
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigToInstance.containsKey(launchConfigName)) {
                if (order == null) {
                    order = svcInstanceIdGenerator.getNextAvailableId(launchConfigName);
                }
                String instanceName = ServiceDiscoveryUtil.generateServiceInstanceName(env,
                        service, launchConfigName, order);
                DeploymentUnitInstance deploymentUnitInstance = context.deploymentUnitInstanceFactory
                        .createDeploymentUnitInstance(context, uuid, service, instanceName, null, null,
                                launchConfigName);
                addDeploymentInstance(launchConfigName, deploymentUnitInstance);
            }
        }
    }

    public void cleanupInstancesWithMissingDependencies() {
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigToInstance.containsKey(launchConfigName)) {
                cleanupInstanceWithMissingDep(launchConfigName);
            }
        }
    }

    protected void cleanupInstanceWithMissingDep(String launchConfigName) {
        List<String> usedInLaunchConfigs = sidekickUsedByMap.get(launchConfigName);
        if (usedInLaunchConfigs == null) {
            return;
        }
        for (String usedInLaunchConfig : usedInLaunchConfigs) {
            DeploymentUnitInstance usedByInstance = launchConfigToInstance.get(usedInLaunchConfig);
            if (usedByInstance == null) {
                continue;
            }
            clenaupDeploymentInstance(usedByInstance);
            cleanupInstanceWithMissingDep(usedInLaunchConfig);
        }
    }

    protected void clenaupDeploymentInstance(DeploymentUnitInstance instance) {
        instance.remove();
        launchConfigToInstance.remove(instance.getLaunchConfigName());

    }

    public List<String> getLaunchConfigNames() {
        return launchConfigNames;
    }

    public Service getService() {
        return service;
    }

    @SuppressWarnings("unchecked")
    public List<String> getSidekickRefs(Service service, String launchConfigName) {
        List<String> configNames = new ArrayList<>();
        for (DeploymentUnit.SidekickType sidekickType : DeploymentUnit.SidekickType.supportedTypes) {
            Object sidekicksLaunchConfigObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                    sidekickType.launchConfigType);
            if (sidekicksLaunchConfigObj != null) {
                if (sidekickType.isList) {
                    configNames.addAll((List<String>) sidekicksLaunchConfigObj);
                } else {
                    configNames.add(sidekicksLaunchConfigObj.toString());
                }
            }
        }

        List<String> toReturn = new ArrayList<>();
        for (String name : configNames) {
            if (name.equalsIgnoreCase(service.getName())) {
                toReturn.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            } else {
                toReturn.add(name);
            }
        }

        return toReturn;
    }

    public boolean hasSidekicks() {
        return launchConfigNames.size() > 1;
    }

}
