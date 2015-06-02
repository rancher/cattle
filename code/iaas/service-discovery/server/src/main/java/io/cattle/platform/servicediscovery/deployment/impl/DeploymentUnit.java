package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeploymentUnit {

    String uuid;
    Map<Long, DeploymentUnitInstance> serviceToInstance = new HashMap<>();
    List<Service> services;
    DeploymentServiceContext context;
    Map<String, String> unitLabels = new HashMap<>();

    public DeploymentUnit() {
    }

    /*
     * This constructor is called to add existing unit
     */
    public DeploymentUnit(DeploymentServiceContext context, String uuid,
            List<Service> services, List<DeploymentUnitInstance> deploymentUnitInstances, Map<String, String> labels) {
        this(context, uuid, services);
        for (DeploymentUnitInstance instance : deploymentUnitInstances) {
            serviceToInstance.put(instance.getService().getId(), instance);
        }
        this.unitLabels = labels;
    }

    protected DeploymentUnit(DeploymentServiceContext context, String uuid, List<Service> services) {
        this.context = context;
        this.uuid = uuid;
        this.services = services;
    }
    
    /*
     * this constructor is called to create a new unit
     */
    public DeploymentUnit(DeploymentServiceContext context, List<Service> services, Map<String, String> labels) {
        this(context, UUID.randomUUID().toString(), services);
        if (labels != null) {
            this.unitLabels.putAll(labels);
        }
    }

    private void createMissingUnitInstances(Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        for (Service service : services) {
            if (!serviceToInstance.containsKey(service.getId())) {
                String instanceName = context.sdService.generateServiceInstanceName(service,
                        svcInstanceIdGenerator.get(service.getId()).getNextAvailableId());
                DeploymentUnitInstance deploymentUnitInstance = context.deploymentUnitInstanceFactory
                        .createDeploymentUnitInstance(context, uuid, service, instanceName, null, null);
                serviceToInstance.put(service.getId(), deploymentUnitInstance);
            }
        }
    }

    public boolean isError() {
        /*
         * This should check for instances with an error transitioning state
         */
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            if (instance.isError()) {
                return true;
            }
        }
        return false;
    }

    public void remove() {
        /*
         * Delete all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            instance.remove();
        }
    }

    public void stop() {
        /*
         * stops all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
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

        for (Long serviceId : serviceToInstance.keySet()) {
            createInstance(serviceId);
        }
    }
    
    public void waitForStart(){
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            instance.waitForStart();
        }
    }

    @SuppressWarnings("unchecked")
    protected DeploymentUnitInstance createInstance(Long serviceId) {
        List<Integer> volumesFromInstanceIds = new ArrayList<>();
        List<Integer> volumesFromServiceIds = DataAccessor
                .fields(context.objectManager.loadResource(Service.class, serviceId)).withKey(
                ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE
                        .getCattleName()).withDefault(Collections.EMPTY_LIST).as(List.class);
        
        for (Integer volumesFromServiceId : volumesFromServiceIds) {
            //check if the service is present in the service map (it can be referenced, but removed already)
            DeploymentUnitInstance volumesFromUnitInstance = serviceToInstance.get(volumesFromServiceId.longValue());
            if (volumesFromUnitInstance != null && volumesFromUnitInstance instanceof InstanceUnit) {
                if (((InstanceUnit) volumesFromUnitInstance).getInstance() == null) {
                    // request new instance creation
                    volumesFromUnitInstance = createInstance(volumesFromUnitInstance.getService().getId());
                }
                // wait for start
                volumesFromUnitInstance.start(new HashMap<String, Object>());
                volumesFromUnitInstance.waitForStart();
                volumesFromInstanceIds.add(((InstanceUnit) volumesFromUnitInstance).getInstance().getId().intValue());
            }
        }
        
        this.serviceToInstance.get(serviceId).start(
                populateDeployParams(this.serviceToInstance.get(serviceId), volumesFromInstanceIds));

        return this.serviceToInstance.get(serviceId);
    }

    public boolean isStarted() {
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    public boolean isUnhealthy() {
        // returns list of instances that need cleanup (having bad health)
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }
        return false;
    }

    public boolean isComplete() {
        return serviceToInstance.size() == services.size();
    }

    protected Map<String, Object> populateDeployParams(DeploymentUnitInstance instance,
            List<Integer> volumesFromInstanceIds) {
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
        return deployParams;
    }

    protected Map<String, String> getLabels(DeploymentUnitInstance instance) {
        Service service = instance.getService();
        Map<String, String> labels = new HashMap<>();
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_NAME, service.getName());
        labels.put(ServiceDiscoveryConstants.LABEL_ENVIRONMENT_NAME,
                context.objectManager.loadResource(Environment.class, service.getEnvironmentId()).getName());
        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, uuid);
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

    public String getUuid() {
        return uuid;
    }
}
