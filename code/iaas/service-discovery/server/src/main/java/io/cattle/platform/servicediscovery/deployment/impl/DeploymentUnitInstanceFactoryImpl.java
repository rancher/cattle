package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceFactory;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class DeploymentUnitInstanceFactoryImpl implements DeploymentUnitInstanceFactory {
    @Inject
    ObjectManager objectMgr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    GenericMapDao mapDao;
    
    @Override
    public DeploymentUnitInstance createDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Object instanceObj, Map<String, String> labels, String launchConfigName) {
        if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
            Instance instance = null;
            if (instanceObj != null) {
                instance = (Instance) instanceObj;
            }
            return new DefaultDeploymentUnitInstance(context, uuid, service,
                    instanceName, instance, labels, launchConfigName);
        } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            LoadBalancerHostMap hostMap = null;
            if (instanceObj != null) {
                hostMap = (LoadBalancerHostMap) instanceObj;
            }
            return new LoadBalancerDeploymentUnitInstance(context, uuid,
                    service, hostMap, labels, launchConfigName);
        } else if (service.getKind().equalsIgnoreCase(KIND.EXTERNALSERVICE.name())) {
            String ip = null;
            if (instanceObj != null) {
                ip = (String) instanceObj;
            }
            return new ExternalDeploymentUnitInstance(uuid, service, context, ip, launchConfigName);
        }
        return null;
    }


    @Override
    public List<DeploymentUnit> collectDeploymentUnits(List<Service> services, DeploymentServiceContext context) {
        /*
         * 1. find all containers related to the service through the serviceexposemaps for regular service, and
         * loadBalancerHostMap for the lb service. Then group all the objects
         * by the label 'io.rancher.deployment.unit'. When containers are deployed through service discovery that
         * label will be placed on them.
         * 
         * 2. put all the containers to the deploymentUnit
         */
        
        Map<String, Map<String, String>> uuidToLabels = new HashMap<>();
        Map<String, List<DeploymentUnitInstance>> uuidToInstances = new HashMap<>();
        List<DeploymentUnit> units = new ArrayList<>();

        for (Service service : services) {
            if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
                collectDefaultServiceInstances(context, uuidToLabels, uuidToInstances, service);
            } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                collectLoadBalancerServiceInstances(context, uuidToLabels, uuidToInstances, service);
            } else if (service.getKind().equalsIgnoreCase(KIND.EXTERNALSERVICE.name())) {
                collectExternalServiceInstances(context, uuidToLabels, uuidToInstances, service);
            }
            for (String uuid : uuidToInstances.keySet()) {
                DeploymentUnit unit = new DeploymentUnit(context, uuid, services, uuidToInstances.get(uuid),
                        uuidToLabels.get(uuid));
                units.add(unit);
            }
        }

        return units;
    }


    @SuppressWarnings("unchecked")
    protected void collectExternalServiceInstances(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        List<String> externalIps = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);

        // 1. request deployment units for ips defined on the service
        for (String externalIp : externalIps) {
            createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, externalIp);
        }

        // 2. get existing maps (they will be cleaned up later if ip is no longer on the service)
        List<? extends ServiceExposeMap> exposeMaps = expMapDao.getNonRemovedServiceIpMaps(service.getId());
        for (ServiceExposeMap exposeMap : exposeMaps) {
            createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, exposeMap.getIpAddress());
        }
    }

    protected void createExternalDeploymentUnit(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service, String externalIp) {
        String uuid = UUID.randomUUID().toString();
        DeploymentUnitInstance unitInstance = createDeploymentUnitInstance(context, uuid, service, null,
                externalIp, null, ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        addToDeploymentUnitList(uuidToLabels, uuidToInstances, new HashMap<String, String>(), uuid,
                unitInstance);
    }


    @SuppressWarnings("unchecked")
    protected void collectLoadBalancerServiceInstances(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        LoadBalancer lb = objectMgr.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);

        List<? extends LoadBalancerHostMap> hostMaps = mapDao.findNonRemoved(LoadBalancerHostMap.class,
                LoadBalancer.class, lb.getId());
        for (LoadBalancerHostMap hostMap : hostMaps) {
            Map<String, Object> data = DataUtils.getFields(hostMap);

            Map<String, String> instanceLabels = data.get(InstanceConstants.FIELD_LABELS) == null ? new HashMap<String, String>()
                    : (Map<String, String>) data.get(InstanceConstants.FIELD_LABELS);
            String deploymentUnitUUID = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
            String launchConfigName = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG);

            DeploymentUnitInstance unitInstance = createDeploymentUnitInstance(context, deploymentUnitUUID, service, null, hostMap,
                    instanceLabels, launchConfigName);

            addToDeploymentUnitList(uuidToLabels, uuidToInstances, instanceLabels, deploymentUnitUUID,
                    unitInstance);
        }
    }


    @SuppressWarnings("unchecked")
    protected void collectDefaultServiceInstances(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        List<? extends Instance> serviceContainers = expMapDao.listServiceInstances(service.getId());
        for (Instance serviceContainer : serviceContainers) {
            Map<String, String> instanceLabels = DataAccessor.fields(serviceContainer)
                    .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
            String deploymentUnitUUID = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
            String launchConfigName = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG);

            DeploymentUnitInstance unitInstance = createDeploymentUnitInstance(context, deploymentUnitUUID,
                    service, serviceContainer.getName(), serviceContainer, instanceLabels, launchConfigName);

            addToDeploymentUnitList(uuidToLabels, uuidToInstances, instanceLabels, deploymentUnitUUID,
                    unitInstance);
        }
    }


    protected void addToDeploymentUnitList(Map<String, Map<String, String>> uuidToLabels,
            Map<String, List<DeploymentUnitInstance>> uuidToInstances, Map<String, String> instanceLabels,
            String deploymentUnitUUID, DeploymentUnitInstance unitInstance) {
        if (uuidToLabels.get(deploymentUnitUUID) == null) {
            uuidToLabels.put(deploymentUnitUUID, instanceLabels);
        }
        
        List<DeploymentUnitInstance> deploymentUnitInstances = uuidToInstances.get(deploymentUnitUUID);
        if (deploymentUnitInstances == null) {
            deploymentUnitInstances = new ArrayList<>();
        }
        deploymentUnitInstances.add(unitInstance);
        uuidToInstances.put(deploymentUnitUUID, deploymentUnitInstances);
    }
}
