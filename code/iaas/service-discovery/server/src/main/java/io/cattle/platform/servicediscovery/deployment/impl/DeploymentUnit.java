package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeploymentUnit {

    String uuid;
    DeploymentServiceContext context;
    Map<String, String> unitLabels = new HashMap<>();
    Map<Long, DeploymentUnitService> svc = new HashMap<>();

    private static List<String> supportedUnitLabels = Arrays
            .asList(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);

    public DeploymentUnit() {
    }

    /*
     * This constructor is called to add existing unit
     */
    public DeploymentUnit(DeploymentServiceContext context, String uuid,
            List<Service> services, List<DeploymentUnitInstance> deploymentUnitInstances, Map<String, String> labels) {
        this(context, uuid, services);
        for (DeploymentUnitInstance instance : deploymentUnitInstances) {
            Service service = instance.getService();
            DeploymentUnitService duService = svc.get(service.getId());
            duService.addDeploymentInstance(instance.getLaunchConfigName(), instance);
        }
        setLabels(labels);
    }

    protected DeploymentUnit(DeploymentServiceContext context, String uuid, List<Service> services) {
        this.context = context;
        this.uuid = uuid;
        for (Service service : services) {
            this.svc.put(service.getId(),
                    new DeploymentUnitService(service, ServiceDiscoveryUtil.getServiceLaunchConfigNames(service),
                            context));
        }
    }

    /*
     * this constructor is called to create a new unit
     */
    public DeploymentUnit(DeploymentServiceContext context, List<Service> services, Map<String, String> labels) {
        this(context, UUID.randomUUID().toString(), services);
        setLabels(labels);
    }

    protected void setLabels(Map<String, String> labels) {
        if (labels != null) {
            for (String label : labels.keySet()) {
                if (supportedUnitLabels.contains(label)) {
                    this.unitLabels.put(label, labels.get(label));
                }
            }
        }
    }

    private void createMissingUnitInstances(Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        for (Long serviceId : svc.keySet()) {
            DeploymentUnitService duService = svc.get(serviceId);
            duService.createMissingInstances(svcInstanceIdGenerator.get(serviceId), uuid);
        }
    }

    public boolean isError() {
        if (!this.isComplete()) {
            return true;
        }
        /*
         * This should check for instances with an error transitioning state
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isError()) {
                return true;
            }
        }
        return false;
    }

    private boolean isHostActive() {
        for (DeploymentUnitInstance deployUnitInstance : getDeploymentUnitInstances()) {
            if (!(deployUnitInstance instanceof InstanceUnit)) {
                // external deployment units do not have instances
                return true;
            }

            Instance instance = ((InstanceUnit)deployUnitInstance).getInstance();
            if (instance != null && instance.getId() != null) {
                // TODO: Performance-wise, this is really bad!  Especially, since we already
                // know what host is going down from the host trigger.

                // Check whether this instance has been deployed and if so, what is the state of the
                // host?
                Host host = context.exposeMapDao.getHostForInstance(instance.getId());
                if (host != null) {
                    if (CommonStatesConstants.REMOVING.equals(host.getState()) ||
                            CommonStatesConstants.REMOVED.equals(host.getState()) ||
                            CommonStatesConstants.PURGING.equals(host.getState()) ||
                            CommonStatesConstants.PURGED.equals(host.getState())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void remove() {
        /*
         * Delete all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.remove();
        }
    }

    public void stop() {
        /*
         * stops all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.stop();
        }
    }

    public void start(Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        /*
         * Start the instances in the correct order depending on the volumes from.
         * Attempt to start things in parallel, but if not possible (like volumes-from) then start each service
         * sequentially.
         * 
         * If there are three services but only two containers, create the third
         * 
         * If one of the containers service health is bad, then create another one (but don't delete the existing).
         * 
         */
        createMissingUnitInstances(svcInstanceIdGenerator);

        for (Long serviceId : svc.keySet()) {
            DeploymentUnitService duService = svc.get(serviceId);
            for (String launchConfigName : duService.getLaunchConfigNames()) {
                createInstance(launchConfigName, duService.getService());
            }
        }

        this.waitForAllocate();
    }
    
    protected void waitForAllocate() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.waitForAllocate();
        }
    }

    public void waitForStart(){
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.waitForStart();
        }
    }

    protected DeploymentUnitInstance createInstance(String launchConfigName, Service service) {
        List<Integer> volumesFromInstanceIds = getVolumesFromInstancesIds(service, launchConfigName);
        Integer networkContainerId = getNetworkContainerId(launchConfigName, service);
        getDeploymentUnitInstance(service, launchConfigName).waitForNotTransitioning();
        getDeploymentUnitInstance(service, launchConfigName)
                .start(
                        populateDeployParams(getDeploymentUnitInstance(service, launchConfigName),
                                volumesFromInstanceIds,
                                networkContainerId));

        return getDeploymentUnitInstance(service, launchConfigName);
    }

    @SuppressWarnings("unchecked")
    protected List<Integer> getVolumesFromInstancesIds(Service service, String launchConfigName) {
        List<Integer> volumesFromInstanceIds = new ArrayList<>();
        Object volumesFromLaunchConfigs = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                ServiceDiscoveryConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);
        Object volumesFromInstance = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                DockerInstanceConstants.FIELD_VOLUMES_FROM);
        if (volumesFromInstance != null) {
            volumesFromInstanceIds.addAll((List<Integer>) volumesFromInstance);
        }
        
        if (volumesFromLaunchConfigs != null) {
            for (String volumesFromLaunchConfig : (List<String>) volumesFromLaunchConfigs) {
                // check if the service is present in the service map (it can be referenced, but removed already)
                if (volumesFromLaunchConfig.toString().equalsIgnoreCase(service.getName())) {
                    volumesFromLaunchConfig = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                DeploymentUnitInstance volumesFromUnitInstance = getDeploymentUnitInstance(service,
                        volumesFromLaunchConfig.toString());
                if (volumesFromUnitInstance != null && volumesFromUnitInstance instanceof InstanceUnit) {
                    if (((InstanceUnit) volumesFromUnitInstance).getInstance() == null) {
                        // request new instance creation
                        volumesFromUnitInstance = createInstance(volumesFromUnitInstance.getLaunchConfigName(), service);
                    }
                    // wait for start
                    volumesFromUnitInstance.start(new HashMap<String, Object>());
                    volumesFromUnitInstance.waitForStart();
                    volumesFromInstanceIds.add(((InstanceUnit) volumesFromUnitInstance).getInstance().getId()
                            .intValue());
                }
            }
        }

        return volumesFromInstanceIds;
    }

    protected Integer getNetworkContainerId(String launchConfigName, Service service) {
        Integer networkContainerId = null;

        Object networkFromInstance = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        if (networkFromInstance != null) {
            return (Integer) networkFromInstance;
        }

        Object networkFromLaunchConfig = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                ServiceDiscoveryConstants.FIELD_NETWORK_LAUNCH_CONFIG);

        if (networkFromLaunchConfig != null) {
            // check if the service is present in the service map (it can be referenced, but removed already)
            if (networkFromLaunchConfig.toString().equalsIgnoreCase(service.getName())) {
                networkFromLaunchConfig = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
            }
            DeploymentUnitInstance networkFromUnitInstance = getDeploymentUnitInstance(service,
                    networkFromLaunchConfig.toString());

            if (networkFromUnitInstance != null && networkFromUnitInstance instanceof InstanceUnit) {
                if (((InstanceUnit) networkFromUnitInstance).getInstance() == null) {
                    // request new instance creation
                    networkFromUnitInstance = createInstance(networkFromUnitInstance.getLaunchConfigName(), service);
                }
                // wait for start
                networkFromUnitInstance.start(new HashMap<String, Object>());
                networkFromUnitInstance.waitForStart();
                networkContainerId = ((InstanceUnit) networkFromUnitInstance).getInstance().getId().intValue();
            }
        }

        return networkContainerId;
    }

    public boolean isStarted() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    public boolean isUnhealthy() {
        // returns list of instances that need cleanup (having bad health)
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }
        if (!isHostActive()) {
            return true;
        }
        return false;
    }

    protected boolean isComplete() {
        for (DeploymentUnitService duService : svc.values()) {
            if (!duService.isComplete()) {
                return false;
            }
        }
        return true;
    }

    protected Map<String, Object> populateDeployParams(DeploymentUnitInstance instance,
            List<Integer> volumesFromInstanceIds, Integer networkContainerId) {
        Map<String, Object> deployParams = new HashMap<>();
        Map<String, String> instanceLabels = getLabels(instance);
        deployParams.put(InstanceConstants.FIELD_LABELS, instanceLabels);
        if (volumesFromInstanceIds != null && !volumesFromInstanceIds.isEmpty()) {
            deployParams.put(DockerInstanceConstants.FIELD_VOLUMES_FROM, volumesFromInstanceIds);
        }
        Object hostId = instanceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostId != null) {
            deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
        }

        if (networkContainerId != null) {
            deployParams.put(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID, networkContainerId);
        }
        return deployParams;
    }

    protected Map<String, String> getLabels(DeploymentUnitInstance instance) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = instance.getService().getName() + '/' + instance.getLaunchConfigName();
        String envName = context.objectManager.loadResource(Environment.class, instance.getService().getEnvironmentId())
                .getName();
        labels.put(ServiceDiscoveryConstants.LABEL_STACK_NAME, envName);
        labels.put(ServiceDiscoveryConstants.LABEL_STACK_SERVICE_NAME, envName + "/" + serviceName);

        // LEGACY: keeping backwards compatibility with 'project'
        labels.put(ServiceDiscoveryConstants.LABEL_PROJECT_NAME, envName);
        labels.put(ServiceDiscoveryConstants.LABEL_PROJECT_SERVICE_NAME, envName + "/" + serviceName);

        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, uuid);

        /*
         * Put label with launch config name
         */
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG, instance.getLaunchConfigName());

        /*
         * Put affinity constraint on every instance to let allocator know that they should go to the same host
         */
        // TODO: Might change labels into a Multimap or add a service function to handle merging
        String containerLabelSoftAffinityKey = ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL + AffinityOps.SOFT_EQ.getLabelSymbol();
        labels.put(containerLabelSoftAffinityKey, ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT + "=" + this.uuid);
        labels.putAll(this.unitLabels);

        return labels;
    }

    public Map<String, String> getLabels() {
        return unitLabels;
    }

    protected List<DeploymentUnitInstance> getDeploymentUnitInstances() {
        List<DeploymentUnitInstance> instances = new ArrayList<>();
        for (Long serviceId : svc.keySet()) {
            DeploymentUnitService duService = svc.get(serviceId);
            instances.addAll(duService.getInstances());
        }
        return instances;
    }

    protected DeploymentUnitInstance getDeploymentUnitInstance(Service service, String launchConfigName) {
        DeploymentUnitService duService = svc.get(service.getId());
        return duService.getInstance(launchConfigName);
    }

}
