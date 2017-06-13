package io.cattle.platform.service.revision.impl;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.RevisionTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.iaas.api.service.RevisionDiffomatic;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.storage.api.filter.ExternalTemplateInstanceFilter;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.resource.UUID;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class RevisionManagerImpl implements RevisionManager {

    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    ServiceDao serviceDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    @Named("CoreSchemaFactory")
    SchemaFactory schemaFactory;
    @Inject
    StorageService storageService;

    @Override
    public Revision createInitialRevision(Service service, Map<String, Object> data) {
        return createInitialRevision(service, service.getAccountId(), data);
    }

    protected Revision createInitialRevision(Service service, long accountId, Map<String, Object> data) {
        Map<String, Object> revisionConfig = new HashMap<>(data);
        RevisionDiffomatic.removeUpdateFields(revisionConfig);

        // There is an assumption this is called from the front end and in a DB transaction
        Revision revision = objectManager.create(Revision.class,
                REVISION.ACCOUNT_ID, accountId,
                REVISION.SERVICE_ID, service == null ? null : service.getId(),
                InstanceConstants.FIELD_REVISION_CONFIG, revisionConfig);
        if (service != null) {
            objectManager.setFields(service,
                    SERVICE.REVISION_ID, revision.getId());
        }

        return revision;
    }

    @Override
    public RevisionDiffomatic createNewRevision(SchemaFactory factory, Service service, Map<String, Object> data) {
        Revision currentRevision = objectManager.loadResource(Revision.class, service.getRevisionId());
        RevisionDiffomatic diffomatic = new RevisionDiffomatic(currentRevision, data, getSchema(factory));
        return diffomatic;
    }

    @Override
    public RevisionDiffomatic createNewRevision(SchemaFactory factory, Instance instance, Map<String, Object> data) {
        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
        if (unit == null) {
            return null;
        }

        Revision currentRevision = objectManager.loadResource(Revision.class,
                unit.getRequestedRevisionId() == null ? unit.getRevisionId() : unit.getRequestedRevisionId());

        data.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        String lcName = DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);
        Map<String, Object> changes = new HashMap<>();

        if (ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(lcName)) {
            changes.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
            changes.put(ServiceConstants.FIELD_LAUNCH_CONFIG, data);
        } else {
            changes.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, Arrays.asList(data));
        }

        RevisionDiffomatic diffomatic = new RevisionDiffomatic(currentRevision, changes, getSchema(factory));
        return diffomatic;
    }

    protected Schema getSchema(SchemaFactory schemaFactory) {
        return schemaFactory.getSchema(ServiceConstants.KIND_SECONDARY_LAUNCH_CONFIG);
    }

    @Override
    public Service assignRevision(RevisionDiffomatic diffomatic, Service service) {
        if (diffomatic.isCreateRevision()) {
            Map<String, Object> data = new HashMap<>(diffomatic.getNewRevisionData());
            RevisionDiffomatic.removeUpdateFields(data);
            Revision newRevision = objectManager.create(Revision.class,
                    REVISION.ACCOUNT_ID, service.getAccountId(),
                    REVISION.SERVICE_ID, service.getId(),
                    InstanceConstants.FIELD_REVISION_CONFIG, data);
            objectManager.setFields(service,
                    SERVICE.PREVIOUS_REVISION_ID, service.getRevisionId(),
                    SERVICE.REVISION_ID, newRevision.getId());
        }

        return service;
    }

    @Override
    public Revision assignRevision(RevisionDiffomatic diffomatic, Instance instance) {
        if (!diffomatic.isCreateRevision()) {
            return null;
        }

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
        Map<String, Object> data = new HashMap<>(diffomatic.getNewRevisionData());
        RevisionDiffomatic.removeUpdateFields(data);
        Revision newRevision = objectManager.create(Revision.class,
                REVISION.ACCOUNT_ID, instance.getAccountId(),
                InstanceConstants.FIELD_REVISION_CONFIG, data);

        objectManager.setFields(unit,
                DEPLOYMENT_UNIT.REQUESTED_REVISION_ID, newRevision.getId());
        processManager.update(unit, null);

        return newRevision;
    }

    @Override
    public void setFieldsForUpgrade(Map<String, Object> data) {
        Map<String, Object> primary = CollectionUtils.toMap(DataAccessor.fromMap(data).withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).get());
        primary.put(ServiceConstants.FIELD_FORCE_UPGRADE, true);
        modifiyImageUuid(primary);

        for (Object o : CollectionUtils.toList(DataAccessor.fromMap(data).withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS).get())) {
            Map<String, Object> map = CollectionUtils.toMap(o);
            map.put(ServiceConstants.FIELD_FORCE_UPGRADE, true);
            modifiyImageUuid(map);
        }
    }

    protected void modifiyImageUuid(Map<String, Object> data) {
        Object imageUuid = data.get(InstanceConstants.FIELD_IMAGE_UUID);
        if (imageUuid == null) {
            return;
        }
        data.put(InstanceConstants.FIELD_IMAGE_UUID, ExternalTemplateInstanceFilter.getImageUuid(imageUuid.toString(), storageService));
    }

    @Override
    public void createInitialRevisionForInstance(long accountId, Map<String, Object> data) {
        setName(data);

        Map<String, Object> revisionData = new HashMap<>(data);
        Long deploymentUnitId = checkNsFields(revisionData);

        if (deploymentUnitId == null) {
            createDeploymentUnit(accountId, deploymentUnitId, revisionData, data, schemaFactory);
        } else {
            joinDeploymentUnit(deploymentUnitId, revisionData, data, schemaFactory);
        }
    }

    private void createDeploymentUnit(long accountId, Long deploymentUnitId, Map<String, Object> lcRevisionData, Map<String, Object> instanceData,
            SchemaFactory schemaFactory) {
        String name = DataAccessor.fromMap(instanceData).withKey(ObjectMetaDataManager.NAME_FIELD).as(String.class);
        long stackId = getStackId(accountId, instanceData);
        Map<String, Object> revisionData = CollectionUtils.asMap(
                ObjectMetaDataManager.NAME_FIELD, name,
                ServiceConstants.FIELD_LAUNCH_CONFIG, instanceData);

        Revision newRevision = createInitialRevision(null, accountId, revisionData);
        DeploymentUnit unit = serviceDao.createDeploymentUnit(accountId, null, stackId, null, "0", newRevision.getId(), true);

        instanceData.put(InstanceConstants.FIELD_LAUNCH_CONFIG_NAME, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        instanceData.put(ServiceConstants.FIELD_VERSION, "0");
        instanceData.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        instanceData.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        instanceData.put(InstanceConstants.FIELD_REVISION_ID, newRevision.getId());
    }

    private long getStackId(long accountId, Map<String, Object> data) {
        Object stackId = data.get(InstanceConstants.FIELD_STACK_ID);
        if (stackId instanceof Number) {
            return ((Number) stackId).longValue();
        }
        return serviceDao.getOrCreateDefaultStack(accountId).getId();
    }

    protected void joinDeploymentUnit(long deploymentUnitId, Map<String, Object> lcRevisionData, Map<String, Object> instanceData,
            SchemaFactory schemaFactory) {
        String name = ObjectUtils.toString(lcRevisionData.get(ObjectMetaDataManager.NAME_FIELD));
        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, deploymentUnitId);
        Long revisionId = unit.getRequestedRevisionId() == null ? unit.getRevisionId() : unit.getRequestedRevisionId();
        Revision revision = objectManager.loadResource(Revision.class, revisionId);
        Map<String, Object> config = DataAccessor.fieldMap(revision, InstanceConstants.FIELD_REVISION_CONFIG);

        if (ServiceUtil.getLaunchConfigExternalNames(config).contains(name)) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, ObjectMetaDataManager.NAME_FIELD);
        }

        Map<String, Object> revisionData = CollectionUtils.asMap(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, Arrays.asList(lcRevisionData));
        RevisionDiffomatic diffomatic = new RevisionDiffomatic(revision, revisionData, schemaFactory.getSchema("secondayLaunchConfig"));

        Revision newRevision = objectManager.create(Revision.class,
                REVISION.ACCOUNT_ID, revision.getAccountId(),
                InstanceConstants.FIELD_REVISION_CONFIG, diffomatic.getNewRevisionData());

        unit.setRequestedRevisionId(newRevision.getId());
        objectManager.persist(unit);

        instanceData.put(InstanceConstants.FIELD_LAUNCH_CONFIG_NAME, name);
        instanceData.put(ServiceConstants.FIELD_VERSION, diffomatic.getNewVersion());
        instanceData.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        instanceData.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        instanceData.put(InstanceConstants.FIELD_REVISION_ID, newRevision.getId());
    }

    protected Long checkNsFields(Map<String, Object> revisionData) {
        Long deploymentUnitId = null;

        for (Map.Entry<String, String> entry : ServiceConstants.NS_DEP_FIELD_MAPPING.entrySet()) {
            String lcName = entry.getKey();
            String field = entry.getValue();

            Object obj = revisionData.get(field);
            if (obj == null) {
                continue;
            }

            Object target = null;

            if (obj instanceof Long) {
                Instance instance = objectManager.loadResource(Instance.class, (Long)obj);
                deploymentUnitId = validateInstance(deploymentUnitId, field, instance);
                target = instance.getName();
            } else if (obj instanceof List<?>) {
                List<String> names = new ArrayList<>();
                for (Object item : (List<?>) obj) {
                    if (item instanceof Long) {
                        Instance instance = objectManager.loadResource(Instance.class, (Long)item);
                        deploymentUnitId = validateInstance(deploymentUnitId, field, instance);
                        names.add(instance.getName());
                    }
                }
                target = names;
            }

            if (target != null && lcName != null) {
                revisionData.put(lcName, target);
            }
            revisionData.remove(field);
        }

        return deploymentUnitId;
    }

    protected Long validateInstance(Long deploymentUnitId, String field, Instance instance) {
        if (instance == null) {
            return deploymentUnitId;
        }

        if (instance.getDeploymentUnitId() == null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    "Field " + field + " refers to legacy or native container", null);
        }

        if (deploymentUnitId != null && !deploymentUnitId.equals(instance.getDeploymentUnitId())) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    "Can not join to two instances in different deployment units", null);
        }

        if (instance.getServiceId() != null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    "Field " + field + " refers to service managed container", null);
        }

        return instance.getDeploymentUnitId();
    }

    protected void setName(Map<String, Object> data) {
        String name = ObjectUtils.toString(data.get(ObjectMetaDataManager.NAME_FIELD));
        if (StringUtils.isBlank(name)) {
            name = "r-" + UUID.randomUUID().toString().substring(0, 8);
            data.put(ObjectMetaDataManager.NAME_FIELD, name);
        }
    }

    @Override
    public Map<String, Object> getServiceDataForRollback(Service service, Long targetRevisionId) {
        if (targetRevisionId == null) {
            targetRevisionId = service.getPreviousRevisionId();
        }

        if (targetRevisionId == null) {
            return null;
        }

        Revision revision = objectManager.loadResource(Revision.class, targetRevisionId);
        if (revision == null || revision.getRemoved() != null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        result.putAll(DataAccessor.fieldMap(revision, InstanceConstants.FIELD_REVISION_CONFIG));
        result.put(InstanceConstants.FIELD_REVISION_ID, targetRevisionId);
        result.put(InstanceConstants.FIELD_PREVIOUS_REVISION_ID, service == null ? null : service.getRevisionId());

        return result;
    }

    @Override
    public Service convertToService(Instance serviceInstance) {
        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, serviceInstance.getDeploymentUnitId());
        List<Instance> instances = objectManager.find(Instance.class,
                INSTANCE.DEPLOYMENT_UNIT_ID, unit.getId(),
                INSTANCE.REMOVED, null);

        Revision revision = objectManager.loadResource(Revision.class, unit.getRequestedRevisionId() == null ?
                unit.getRevisionId() : unit.getRequestedRevisionId());
        Map<String, Object> serviceData = getServiceDataForRollback(null, revision.getId());
        serviceData.put(ObjectMetaDataManager.NAME_FIELD, serviceInstance.getName());
        serviceData.put(ObjectMetaDataManager.ACCOUNT_FIELD, serviceInstance.getAccountId());
        serviceData.put(ServiceConstants.FIELD_SCALE, 1);
        serviceData.put(ServiceConstants.FIELD_BATCHSIZE, 1);
        serviceData.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, 2000L);
        serviceData.put(InstanceConstants.FIELD_STACK_ID, serviceInstance.getStackId());
        serviceData.put(ServiceConstants.FIELD_START_ON_CREATE, true);

        Service service = resourceDao.createAndSchedule(Service.class, serviceData);
        for (Instance instance : instances) {
            instance.setServiceId(service.getId());
            objectManager.persist(instance);

            objectManager.create(ServiceExposeMap.class,
                    SERVICE_EXPOSE_MAP.STATE, CommonStatesConstants.ACTIVE,
                    SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                    SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(),
                    SERVICE_EXPOSE_MAP.ACCOUNT_ID, instance.getAccountId(),
                    SERVICE_EXPOSE_MAP.DNS_PREFIX, null,
                    SERVICE_EXPOSE_MAP.MANAGED, true);
        }

        revision.setServiceId(service.getId());
        objectManager.persist(revision);

        unit.setServiceId(service.getId());
        unit.setServiceIndex("1");
        objectManager.persist(unit);

        return service;
    }

    @Override
    public void leaveDeploymentUnit(Instance instance) {
        if (instance.getServiceId() != null || instance.getDeploymentUnitId() == null || instance.getRevisionId() == null
                || StringUtils.isBlank(instance.getName()) || !instance.getDesired()) {
            return;
        }

        Revision revision = objectManager.loadResource(Revision.class, instance.getRevisionId());
        if (revision == null) {
            return;
        }

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());

        String name = DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);

        if (ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(name)) {
            processManager.remove(unit, null);
        } else {
            RevisionDiffomatic diffomatic = new RevisionDiffomatic(revision,
                    removeSecondaryLcConfig(name), getSchema(schemaFactory));
            Revision newRevision = objectManager.create(Revision.class,
                    REVISION.ACCOUNT_ID, instance.getAccountId(),
                    InstanceConstants.FIELD_REVISION_CONFIG, diffomatic.getNewRevisionData());
            unit.setRequestedRevisionId(newRevision.getId());
            objectManager.persist(unit);
        }
    }

    protected Map<String, Object> removeSecondaryLcConfig(String name) {
        return CollectionUtils.asMap(
                ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS,
                Arrays.asList(
                        CollectionUtils.asMap(
                                ObjectMetaDataManager.NAME_FIELD, name,
                                InstanceConstants.FIELD_IMAGE_UUID, ServiceConstants.IMAGE_NONE)));
    }

}
