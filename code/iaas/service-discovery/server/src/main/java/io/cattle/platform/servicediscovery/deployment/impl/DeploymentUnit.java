package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeploymentUnit {

    String uuid;
    Map<Long, DeploymentUnitInstance> serviceToInstance = new HashMap<>();
    DeploymentServiceContext context;

    public DeploymentUnit() {
    }

    public DeploymentUnit(String uuid, Map<Long, DeploymentUnitInstance> deploymentInstances,
            DeploymentServiceContext context) {
        this.uuid = uuid;
        serviceToInstance.putAll(deploymentInstances);
        this.context = context;
    }
    
    public DeploymentUnit(List<Service> services,
            Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator,
            DeploymentServiceContext context) {
        this.uuid = UUID.randomUUID().toString();
        this.createMissingUnitInstances(services, svcInstanceIdGenerator, context);
        this.context = context;
    }

    public void createMissingUnitInstances(List<Service> services,
            Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator,
            DeploymentServiceContext context) {
        for (Service service : services) {
            if (!serviceToInstance.containsKey(service.getId())) {
                DeploymentUnitInstance deploymentUnitInstance = context.deploymentUnitInstanceFactory
                        .createDeploymentUnitInstance(uuid, service, 
                                context.sdService.generateServiceInstanceName(service,
                                        svcInstanceIdGenerator.get(service.getId()).getNextAvailableId()),
                                null, context);
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

    public void start() {
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
        for (Long serviceId : serviceToInstance.keySet()) {
            createInstance(serviceId);
        }

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
            DeploymentUnitInstance volumesFromUnitInstance = this.serviceToInstance
                    .get(volumesFromServiceId.longValue());
            if (volumesFromUnitInstance != null) {
                if (volumesFromUnitInstance.getInstance() == null) {
                    // request new instance creation
                    volumesFromUnitInstance = createInstance(volumesFromUnitInstance.getService().getId());
                }
                // wait for start
                volumesFromUnitInstance.start(new ArrayList<Integer>());
                volumesFromUnitInstance.waitForStart();
                volumesFromInstanceIds.add(volumesFromUnitInstance.getInstance().getId().intValue());
            }
        }
        this.serviceToInstance.get(serviceId).start(volumesFromInstanceIds);

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

    public boolean needsCleanup() {
        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            if (instance.needsCleanup()) {
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        /*
         * Delete any containers that have a bad service health. Do this non-blocking.
         */
    }

    public Collection<DeploymentUnitInstance> getInstances() {
        return serviceToInstance.values();
    }
}
