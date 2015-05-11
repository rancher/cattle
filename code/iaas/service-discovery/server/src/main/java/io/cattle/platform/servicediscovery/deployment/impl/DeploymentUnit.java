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
    Map<Service, DeploymentUnitInstance> serviceToInstance = new HashMap<>();

    public DeploymentUnit() {
    }

    public DeploymentUnit(String uuid, Map<Service, DeploymentUnitInstance> deploymentInstances,
            DeploymentServiceContext context) {
        this.uuid = uuid;
        serviceToInstance.putAll(deploymentInstances);
    }
    
    public DeploymentUnit(List<Service> services,
            Map<Service, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator,
            DeploymentServiceContext context) {
        this.uuid = UUID.randomUUID().toString();
        this.createMissingUnitInstances(services, svcInstanceIdGenerator, context);
    }

    public void createMissingUnitInstances(List<Service> services,
            Map<Service, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator,
            DeploymentServiceContext context) {
        for (Service service : services) {
            if (!serviceToInstance.containsKey(service)) {
                DeploymentUnitInstance deploymentUnitInstance = context.deploymentUnitInstanceFactory
                        .createDeploymentUnitInstance(uuid, service, 
                                context.sdService.generateServiceInstanceName(service,
                                        svcInstanceIdGenerator.get(service).getNextAvailableId()),
                                null, context);
                serviceToInstance.put(service, deploymentUnitInstance);
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
        for (Service service : serviceToInstance.keySet()) {
            createInstance(serviceToInstance, service, false);
        }

        for (DeploymentUnitInstance instance : serviceToInstance.values()) {
            instance.waitForStart();
        }
    }

    @SuppressWarnings("unchecked")
    protected DeploymentUnitInstance createInstance(Map<Service, DeploymentUnitInstance> serviceToInstance,
            Service service, boolean waitForStart) {
        List<Integer> volumesFromInstanceIds = new ArrayList<>();
        List<Integer> volumesFromServiceIds = DataAccessor.fields(service).withKey(
                ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE
                        .getCattleName()).withDefault(Collections.EMPTY_LIST).as(List.class);
        
        for (Integer volumesFromServiceId : volumesFromServiceIds) {
            DeploymentUnitInstance volumesFromUnitInstance = getInstanceByServiceId(volumesFromServiceId.longValue());
            if (volumesFromUnitInstance.getInstance() != null) {
                // get instanceId from existing instance
                volumesFromInstanceIds.add(volumesFromUnitInstance.getInstance().getId().intValue());
            } else {
                // request new instance creation, and put waitForStart=true
                volumesFromUnitInstance = createInstance(serviceToInstance, volumesFromUnitInstance.getService(), true);
                volumesFromInstanceIds.add(volumesFromUnitInstance.getInstance().getId().intValue());
            }
        }
        serviceToInstance.get(service).start(volumesFromInstanceIds);
        if (waitForStart) {
            serviceToInstance.get(service).waitForStart();
        }
        return serviceToInstance.get(service);
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

    protected DeploymentUnitInstance getInstanceByServiceId(long serviceId) {
        for (Service service : serviceToInstance.keySet()) {
            if (service.getId().equals(serviceId)) {
                return serviceToInstance.get(service);
            }
        }
        return null;
    }

}
