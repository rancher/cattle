package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.*;
import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SampleDataStartupV15 extends AbstractSampleData {
    private static final String CONFIG_NAME = "deployment-unit-update";

    @Inject
    ServiceDao svcDao;

    @Override
    protected String getName() {
        return "sampleDataVersion14";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        createMissingDeploymentUnits();
        updateStateAndHealthState();
        updateDeploymentUnitAndServiceId();
    }

    public void updateDeploymentUnitAndServiceId() {
        List<? extends DeploymentUnit> dus = objectManager.find(DeploymentUnit.class, DEPLOYMENT_UNIT.REMOVED,
                new Condition(ConditionType.NULL));
        Map<String, Long> duUUIDToId = new HashMap<>();
        for (DeploymentUnit du : dus) {
            duUUIDToId.put(du.getUuid(), du.getId());
        }

        List<? extends ServiceExposeMap> exposeMaps = objectManager.find(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.REMOVED,
                new Condition(ConditionType.NULL), SERVICE_EXPOSE_MAP.MANAGED, true);
        Map<Long, Long> instanceIdToServiceId = new HashMap<>();
        for (ServiceExposeMap exposeMap : exposeMaps) {
            instanceIdToServiceId.put(exposeMap.getInstanceId(), exposeMap.getServiceId());
        }

        List<? extends Instance> instances = objectManager.find(Instance.class, INSTANCE.DEPLOYMENT_UNIT_UUID,
                new Condition(
                ConditionType.NOTNULL), INSTANCE.REMOVED,
                new Condition(ConditionType.NULL));

        List<? extends Service> services = objectManager.find(Service.class, SERVICE.REMOVED, new Condition(
                ConditionType.NULL));
        Map<Long, Long> serviceIdToStackId = new HashMap<>();
        for (Service service : services) {
            serviceIdToStackId.put(service.getId(), service.getStackId());
        }

        for (Instance instance : instances) {
            Long duId = duUUIDToId.get(instance.getDeploymentUnitUuid());
            if (duId == null) {
                continue;
            }
            Map<String, Object> props = new HashMap<>();
            props.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, duId);
            Long serviceId = instanceIdToServiceId.get(instance.getId());
            if (serviceId != null) {
                props.put(InstanceConstants.FIELD_SERVICE_ID, instanceIdToServiceId.get(instance.getId()));
                props.put(InstanceConstants.FIELD_STACK_ID, serviceIdToStackId.get(serviceId));
            }

            objectManager.setFields(instance, props);
        }
    }

    public void updateStateAndHealthState() {
        List<? extends DeploymentUnit> dus = objectManager.find(DeploymentUnit.class, DEPLOYMENT_UNIT.REMOVED, new Condition(ConditionType.NULL));
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.REMOVED, new Condition(ConditionType.NULL));
        Map<Long, String> servicesStatesMap = new HashMap<>();
        for (Service service : services) {
            servicesStatesMap.put(service.getId(), service.getState());
        }

        List<DeploymentUnit> triggerUpdate = new ArrayList<>();
        for (DeploymentUnit du : dus) {
            Map<String, Object> props = new HashMap<>();
            props.put("healthState", HealthcheckConstants.HEALTH_STATE_HEALTHY);
            String duState = null;
            String svcState = servicesStatesMap.get(du.getServiceId());
            List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVE,
                    CommonStatesConstants.UPDATING_ACTIVE, CommonStatesConstants.ACTIVATING);
            if (activeStates.contains(svcState)){
                duState = CommonStatesConstants.ACTIVE;
                triggerUpdate.add(du);
            } else {
                duState = CommonStatesConstants.INACTIVE;
            }

            props.put("state", duState);
            objectManager.setFields(du, props);
        }
        updateConfigItemStatus(triggerUpdate);
    }

    protected void updateConfigItemStatus(List<DeploymentUnit> units) {
        for (DeploymentUnit unit : units) {
            ConfigItemStatus existing = objectManager.findAny(ConfigItemStatus.class, CONFIG_ITEM_STATUS.NAME,
                    CONFIG_NAME, CONFIG_ITEM_STATUS.RESOURCE_ID, unit.getId(), CONFIG_ITEM_STATUS.DEPLOYMENT_UNIT_ID,
                    unit.getId(), CONFIG_ITEM_STATUS.RESOURCE_TYPE, "deployment_unit_id");
            if (existing == null) {
                try {
                    Map<String, Object> props = new HashMap<>();
                    props.put("name", CONFIG_NAME);
                    props.put("requestedVersion", 1);
                    props.put("appliedVersion", 0);
                    props.put("sourceVersion", "");
                    props.put("resourceId", unit.getId());
                    props.put("deploymentUnitId", unit.getId());
                    props.put("resourceType", "deployment_unit_id");
                    props.put("requestedUpdated", new Date(System.currentTimeMillis()));
                    objectManager.create(ConfigItemStatus.class, props);
                } catch (Exception ex) {
                }
            }
        }
    }

    public void createMissingDeploymentUnits() {
        // 1. fetch all service instances with either null deployment_unit_uuid or uuid not having reference in
        // deployment_unit table
        Map<String, Map<String, Object>> duData = new HashMap<>();

        Map<Long, List<Instance>> serviceIdToInstance = svcDao.getServiceInstancesWithNoDeploymentUnit();
        Map<String, String> duToIndex = getDuToServiceIndex(serviceIdToInstance);
        for (Long serviceId : serviceIdToInstance.keySet()) {
            for (Instance instance : serviceIdToInstance.get(serviceId)) {
                String duUUID = getDuUUIDFromLabels(instance);
                if (StringUtils.isEmpty(duUUID)) {
                    continue;
                }

                if (StringUtils.isEmpty(instance.getDeploymentUnitUuid())) {
                    Map<String, Object> props = new HashMap<>();
                    props.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, duUUID);
                    objectManager.setFields(instance, props);
                }
                if (duData.containsKey(duUUID)) {
                    continue;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("serviceId", serviceId);
                data.put("accountId", instance.getAccountId());
                data.put("uuid", duUUID);
                data.put("serviceIndex", duToIndex.get(duUUID));
                Long requestedHostId = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_REQUESTED_HOST_ID);
                if (requestedHostId != null) {
                    Map<String, Object> unitLabels = new HashMap<>();
                    unitLabels.put(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID, String.valueOf(requestedHostId));
                    data.put("labels", unitLabels);
                }
                duData.put(duUUID, data);
            }
        }

        // create missing deploymenUnit object
        for (String uuid : duData.keySet()) {
            DeploymentUnit du = objectManager.findAny(DeploymentUnit.class, DEPLOYMENT_UNIT.UUID, uuid);
            if (du == null) {
                Map<String, Object> data = duData.get(uuid);
                objectManager.create(DeploymentUnit.class, data);
            }
        }
    }

    private Map<String, String> getDuToServiceIndex(Map<Long, List<Instance>> serviceIdToInstance) {
        Map<String, String> duToIndex = new HashMap<>();
        Map<Long, Set<String>> serviceIdToUsedIndexes = new HashMap<>();
        for (Long serviceId : serviceIdToInstance.keySet()) {
            List<Instance> instances = serviceIdToInstance.get(serviceId);
            for (Instance instance : instances) {
                String duUUID = getDuUUIDFromLabels(instance);
                if (StringUtils.isEmpty(duUUID)) {
                    continue;
                }
                String serviceIndex = ServiceConstants.getServiceIndexFromInstanceName(instance.getName());
                Set<String> usedIndexes = new HashSet<>();
                if (serviceIdToUsedIndexes.get(serviceId) != null) {
                    usedIndexes = serviceIdToUsedIndexes.get(serviceId);
                }
                if (usedIndexes.contains(serviceIndex)) {
                    continue;
                }
                usedIndexes.add(serviceIndex);
                serviceIdToUsedIndexes.put(serviceId, usedIndexes);
                duToIndex.put(duUUID, serviceIndex);
            }
        }
        return duToIndex;
    }

    @SuppressWarnings("unchecked")
    public String getDuUUIDFromLabels(Instance instance) {
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .as(Map.class);
        String duUUID = labels.get(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT);
        return duUUID;
    }

}
