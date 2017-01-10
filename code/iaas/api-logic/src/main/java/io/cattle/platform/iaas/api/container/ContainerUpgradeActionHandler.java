package io.cattle.platform.iaas.api.container;

import static io.cattle.platform.core.model.tables.InstanceRevisionTable.*;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.ContainerUpgrade;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ContainerUpgradeActionHandler implements ActionHandler {
    @Inject
    JsonMapper jsonMapper;
    @Inject
    InstanceDao instanceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public String getName() {
        return "instance.upgrade";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Instance originalInstance = (Instance)obj;
        Map<String, Object> data = getAndValidateInstanceData(request, originalInstance);
        Instance upgradedInstance = replaceContainer(originalInstance, data);
        return objectManager.reload(upgradedInstance);
    }

    public Instance replaceContainer(Instance originalInstance, Map<String, Object> data) {
        // create revision if revisionId is not passed
        if (data.get(InstanceConstants.FIELD_REVISION_ID) == null) {
            InstanceRevision revision = instanceDao.createRevision(originalInstance, data, false);
            data.put(InstanceConstants.FIELD_REVISION_ID, revision.getId());
        }
        // create new container record, do not start.
        Instance upgradedInstance = objectManager.create(Instance.class, data);
        if (data.get(InstanceConstants.FIELD_IMAGE_PRE_PULL) != null
                && Boolean.valueOf(data.get(InstanceConstants.FIELD_IMAGE_PRE_PULL).toString())) {
            if (data.get(InstanceConstants.FIELD_IMAGE_UUID) != null) {
                prepullInstanceImage(upgradedInstance, data.get(InstanceConstants.FIELD_IMAGE_UUID).toString());
            }
        }

        // replace revisions of the old containers to use new containers ids
        for (InstanceRevision revision : objectManager.find(InstanceRevision.class, INSTANCE_REVISION.INSTANCE_ID,
                originalInstance.getId())) {
            objectManager.setFields(revision, "instanceId", upgradedInstance.getId());
        }
        // remove old container, start the new one
        if (!(originalInstance.getState().equals(CommonStatesConstants.REMOVED) || originalInstance.getState().equals(
                CommonStatesConstants.REMOVING))) {
            try {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, originalInstance,
                        data);
            } catch (ProcessCancelException e) {
                Map<String, Object> processData = new HashMap<>();
                processData.put(InstanceConstants.PROCESS_STOP + ProcessLogic.CHAIN_PROCESS,
                        InstanceConstants.PROCESS_REMOVE);
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP,
                        originalInstance, processData);
            }
        }
        objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, upgradedInstance,
                data);
        return upgradedInstance;
    }

    public Map<String, Object> getAndValidateInstanceData(ApiRequest request, Instance instance) {
        if (instance.getServiceId() != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Service container upgrade should be done through service");
        }
        
        ContainerUpgrade upgrade = jsonMapper.convertValue(
                request.getRequestObject(),
                io.cattle.platform.core.addon.ContainerUpgrade.class);

        if (upgrade.getConfig() == null && upgrade.getRevisionId() == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    "Either config or revisionId have to be passed in");
        } else if (upgrade.getConfig() != null && upgrade.getRevisionId() != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    "Config and revisionId are mutually exclusive");
        }
        
        Map<String, Object> data = new HashMap<>();
        if (upgrade.getRevisionId() != null) {
            InstanceRevision revision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                    instance.getRevisionId());
            if (!revision.getInstanceId().equals(instance.getId())) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Referenced revision doesn't belong to the instance");
            }
            data.putAll(DataAccessor.fieldMap(revision, InstanceConstants.FIELD_INSTANCE_REVISION_CONFIG));
            data.put(InstanceConstants.FIELD_REVISION_ID, upgrade.getRevisionId());
        } else {
            data.putAll(CollectionUtils.toMap(upgrade.getConfig()));
            data.put(ObjectMetaDataManager.KIND_FIELD, InstanceConstants.KIND_CONTAINER);
        }
        data.put(InstanceConstants.FIELD_STACK_ID, instance.getStackId());
        data.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        data.put(InstanceConstants.FIELD_REPLACEMNT_FOR_INSTANCE_ID, instance.getId());
        data.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, instance.getDeploymentUnitId());
        return data;
    }

    @SuppressWarnings("unchecked")
    protected void prepullInstanceImage(Instance instance, String imageUuid) {
        List<String> images = Arrays.asList(imageUuid);

        List<GenericObject> pullTasks = instanceDao.getImagePullTasks(instance.getAccountId(), images,
                DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(Map.class));
        for (GenericObject pullTask : pullTasks) {
            if (pullTask.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, pullTask, null);
            }
        }
        // TODO - figure out the way to wait for image pre-pull as all objects in handler
        // get created inside a transaction, and waiting for the active state on the object won't work
    }

}
