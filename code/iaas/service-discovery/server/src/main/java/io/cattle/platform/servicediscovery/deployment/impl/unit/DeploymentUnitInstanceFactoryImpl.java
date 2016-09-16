package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
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

import org.apache.commons.lang3.tuple.Pair;

public class DeploymentUnitInstanceFactoryImpl implements DeploymentUnitInstanceFactory {
    @Inject
    ObjectManager objectMgr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    GenericMapDao mapDao;

    @Override
    @SuppressWarnings("unchecked")
    public DeploymentUnitInstance createDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Object instanceObj, String launchConfigName) {
        if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_SERVICE)) {
            Instance instance = null;
            if (instanceObj != null) {
                instance = (Instance) instanceObj;
            }
            return new DefaultDeploymentUnitInstance(context, uuid, service,
                    instanceName, instance, launchConfigName);
        } else if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_LOAD_BALANCER_SERVICE)) {
            Instance instance = null;
            if (instanceObj != null) {
                instance = (Instance) instanceObj;
            }
            return new LoadBalancerDeploymentUnitInstance(context, uuid, service,
                    instanceName, instance, launchConfigName);
        }else if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_EXTERNAL_SERVICE)) {
            Pair<String, String> ipHostName = null;
            if (instanceObj != null) {
                ipHostName = (Pair<String, String>) instanceObj;
            }
            return new ExternalDeploymentUnitInstance(context, uuid, service, launchConfigName, ipHostName.getLeft(),
                    ipHostName.getRight());
        }
        return null;
    }


    @Override
    public List<DeploymentUnit> collectDeploymentUnits(Service service, DeploymentServiceContext context) {
        /*
         * 1. find all containers related to the service through the serviceexposemaps
         * Then group all the objects
         * by the label 'io.rancher.deployment.unit'. When containers are deployed through service discovery that
         * label will be placed on them.
         *
         * 2. put all the containers to the deploymentUnit
         */

        Map<String, Map<String, String>> uuidToLabels = new HashMap<>();
        Map<String, List<DeploymentUnitInstance>> uuidToInstances = new HashMap<>();
        List<DeploymentUnit> units = new ArrayList<>();

        if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_LOAD_BALANCER_SERVICE)) {
            collectDefaultServiceInstances(context, uuidToLabels, uuidToInstances, service);
        } else if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_EXTERNAL_SERVICE)) {
            collectExternalServiceInstances(context, uuidToLabels, uuidToInstances, service);
        }
        for (String uuid : uuidToInstances.keySet()) {
            DeploymentUnit unit = new DeploymentUnit(context, uuid, service, uuidToInstances.get(uuid),
                    uuidToLabels.get(uuid));
            units.add(unit);
        }

        return units;
    }


    protected void collectExternalServiceInstances(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {

        // 1. request deployment units for ips defined on the service
        createExternalUnitsForIps(context, uuidToLabels, uuidToInstances, service);

        // 2. request deployment units for hostname defined on the service
        createDeploymentUnitsForHostname(context, uuidToLabels, uuidToInstances, service);

    }


    protected void createDeploymentUnitsForHostname(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        String hostName = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_HOSTNAME).as(String.class);
        if (hostName != null) {
            createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, null, hostName);
        }

        // get existing maps (they will be cleaned up later if ip is no longer on the service)
        List<? extends ServiceExposeMap> exposeMaps = expMapDao.getNonRemovedServiceHostnameMaps(service.getId());
        for (ServiceExposeMap exposeMap : exposeMaps) {
                createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, null,
                        exposeMap.getHostName());
        }
    }


    @SuppressWarnings("unchecked")
    protected void createExternalUnitsForIps(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        List<String> externalIps = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);

        if (externalIps != null) {
            for (String externalIp : externalIps) {
                createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, externalIp, null);
            }
        }

        // get existing maps (they will be cleaned up later if ip is no longer on the service)
        List<? extends ServiceExposeMap> exposeMaps = expMapDao.getNonRemovedServiceIpMaps(service.getId());
        for (ServiceExposeMap exposeMap : exposeMaps) {
                createExternalDeploymentUnit(context, uuidToLabels, uuidToInstances, service, exposeMap.getIpAddress(),
                    null);
        }
    }

    protected void createExternalDeploymentUnit(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service, String externalIp, String hostName) {
        String uuid = UUID.randomUUID().toString();
        DeploymentUnitInstance unitInstance = createDeploymentUnitInstance(context, uuid, service, null,
                Pair.of(externalIp, hostName), ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        addToDeploymentUnitList(uuidToLabels, uuidToInstances, new HashMap<String, String>(), uuid,
                unitInstance);
    }

    @SuppressWarnings("unchecked")
    protected void collectDefaultServiceInstances(DeploymentServiceContext context,
            Map<String, Map<String, String>> uuidToLabels, Map<String, List<DeploymentUnitInstance>> uuidToInstances,
            Service service) {
        List<? extends Instance> serviceContainers = expMapDao.listServiceManagedInstancesAll(service);
        for (Instance serviceContainer : serviceContainers) {
            Map<String, String> instanceLabels = DataAccessor.fields(serviceContainer)
                    .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
            String deploymentUnitUUID = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
            String launchConfigName = instanceLabels
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG);

            DeploymentUnitInstance unitInstance = createDeploymentUnitInstance(context, deploymentUnitUUID,
                    service, serviceContainer.getName(), serviceContainer, launchConfigName);

            addToDeploymentUnitList(uuidToLabels, uuidToInstances, instanceLabels, deploymentUnitUUID,
                    unitInstance);
        }

        List<? extends io.cattle.platform.core.model.DeploymentUnit> units = context.objectManager.find(
                io.cattle.platform.core.model.DeploymentUnit.class,
                DEPLOYMENT_UNIT.ACCOUNT_ID,
                service.getAccountId(), DEPLOYMENT_UNIT.REMOVED, null, DEPLOYMENT_UNIT.SERVICE_ID, service.getId());
        for (io.cattle.platform.core.model.DeploymentUnit unit : units) {
            if (!uuidToInstances.containsKey(unit.getUuid())) {
                Map<String, String> labels = new HashMap<>();
                Map<String, Object> labelsObj = DataAccessor.fieldMap(unit,
                        InstanceConstants.FIELD_LABELS);
                for (String key : labelsObj.keySet()) {
                    labels.put(key, labelsObj.get(key).toString());
                }
                addToDeploymentUnitList(uuidToLabels, uuidToInstances, labels, unit.getUuid(),
                        null);

            }
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
        if (unitInstance != null) {
            deploymentUnitInstances.add(unitInstance);
        }
        uuidToInstances.put(deploymentUnitUUID, deploymentUnitInstances);
    }
}
