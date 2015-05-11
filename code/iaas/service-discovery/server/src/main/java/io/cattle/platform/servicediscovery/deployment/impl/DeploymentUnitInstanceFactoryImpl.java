package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
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

import javax.inject.Inject;

public class DeploymentUnitInstanceFactoryImpl implements DeploymentUnitInstanceFactory {
    @Inject
    ObjectManager objectMgr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    GenericMapDao mpDao;
    
    @Override
    public DeploymentUnitInstance createDeploymentUnitInstance(String uuid, Service service,
            String instanceName, Object instanceObj, DeploymentServiceContext context) {
        if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
            Instance instance = null;
            if (instanceObj != null) {
                instance = (Instance) instanceObj;
            }
            return new DefaultDeploymentUnitInstance(uuid, service, instanceName,
                    instance,
                    context);
        } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            LoadBalancerHostMap hostMap = null;
            if (instanceObj != null) {
                hostMap = (LoadBalancerHostMap) instanceObj;
            }
            return new LoadBalancerDeploymentUnitInstance(uuid, service,
                    hostMap, context);
        }
        return null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<DeploymentUnitInstance> collectServiceInstances(Service service, DeploymentServiceContext context) {
        /*
         * find all containers related to the services through the serviceexposemaps for regular service, and
         * loadBalancerHostMap for the lb service. Then group all the objects
         * by the label 'io.rancher.deployment.unit'. When containers are deployed through service discovery that
         * label will be placed on them.
         * 
         * If no 'io.rancher.deployment.unit' label is found on the container, generate a random UUID for the value
         * and use that
         */

        List<DeploymentUnitInstance> deploymentUnitInstances = new ArrayList<>();
        if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
            List<? extends Instance> serviceContainers = expMapDao.listServiceInstances(service.getId());
            for (Instance serviceContainer : serviceContainers) {
                Map<String, String> instanceLabels = DataAccessor.fields(serviceContainer)
                        .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
                String deploymentUnitUUID = instanceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
                deploymentUnitInstances
                        .add(createDeploymentUnitInstance(deploymentUnitUUID, service, serviceContainer.getName(),
                                serviceContainer, context));
            }
        } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            LoadBalancer lb = objectMgr.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                    LOAD_BALANCER.REMOVED, null);

            List<? extends LoadBalancerHostMap> hostMaps = mpDao.findNonRemoved(LoadBalancerHostMap.class,
                    LoadBalancer.class, lb.getId());
            for (LoadBalancerHostMap hostMap : hostMaps) {
                Map<String, Object> launchConfig = DataAccessor
                        .fields(hostMap)
                        .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                        .as(Map.class);

                Map<String, String> instanceLabels = launchConfig.get(InstanceConstants.FIELD_LABELS) == null ? new HashMap<String, String>()
                        : (Map<String, String>) launchConfig.get(InstanceConstants.FIELD_LABELS);
                String deploymentUnitUUID = instanceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
                deploymentUnitInstances
                        .add(createDeploymentUnitInstance(deploymentUnitUUID, service, null, hostMap, context));
            }
        }

        return deploymentUnitInstances;
    }
}
