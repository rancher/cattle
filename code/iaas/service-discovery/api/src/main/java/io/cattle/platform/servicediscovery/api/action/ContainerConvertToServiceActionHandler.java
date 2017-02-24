package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.ConvertToServiceInput;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.servicediscovery.api.lock.ConvertToServiceLock;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class ContainerConvertToServiceActionHandler implements ActionHandler {

    @Inject
    ObjectManager objMgr;
    @Inject
    LockManager lockMgr;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    InstanceDao instanceDao;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public String getName() {
        return InstanceConstants.ACTIONT_CONVERT_TO_SERVICE;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Instance)) {
            return null;
        }

        Instance instance = (Instance) obj;

        if (instance.getServiceId() != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is already a part of the service");
        }

        if (instance.getRevisionId() == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is not qualified for the conversion");
        }
        
        final ConvertToServiceInput input = jsonMapper.convertValue(
                request.getRequestObject(),
                ConvertToServiceInput.class);
        String serviceName = input.getName();
        Long stackId = instance.getStackId();
        if (!objMgr.find(Service.class, SERVICE.ACCOUNT_ID, instance.getAccountId(), SERVICE.NAME, serviceName,
                SERVICE.STACK_ID, stackId).isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "Service name " + serviceName + " already exists in the stack");
        }

        List<? extends Instance> instances = objMgr.find(Instance.class, INSTANCE.DEPLOYMENT_UNIT_ID,
                instance.getDeploymentUnitId());
        List<Instance> secondary = new ArrayList<>();
        for (Instance sec : instances) {
            if (sec.getId().equals(instance.getId())) {
                continue;
            }
            if (!instance.getStackId().equals(stackId)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Sidekick container uuid " + instance.getUuid()
                                + " belongs to a different stack, can't convert");
            }
            if (sec.getName() == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Sidekick container uuid " + instance.getUuid() + " is missing name");
            }
            secondary.add(sec);
        }

        return convertToService(serviceName, stackId, instance, secondary);
    }
    
    protected Service convertToService(String name, Long stackId, Instance primary, List<Instance> secondary) {
        DeploymentUnit du = objMgr.loadResource(DeploymentUnit.class, primary.getDeploymentUnitId());
        return lockMgr.lock(new ConvertToServiceLock(du), new LockCallback<Service>() {
                @Override
            public Service doWithLock() {
                Service service = createService(name, stackId, primary, secondary);
                updateDeploymentUnit(du, service);
                updateInstances(stackId, primary, secondary, service);
                return service;
                }

        });
    }

    public void updateInstances(Long stackId, Instance primary, List<Instance> secondary, Service service) {
        List<Instance> instances = new ArrayList<>();
        instances.add(primary);
        instances.addAll(secondary);
        for (Instance instance : instances) {
            objMgr.setFields(instance, INSTANCE.SERVICE_ID, service.getId(), INSTANCE.STACK_ID, stackId);
        }
    }

    public void updateDeploymentUnit(DeploymentUnit du, Service service) {
        objMgr.setFields(du, DEPLOYMENT_UNIT.SERVICE_ID, service.getId());
    }

    public Service createService(String name, Long stackId, Instance primary, List<Instance> secondary) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.NAME_FIELD, name);
        data.put(ObjectMetaDataManager.KIND_FIELD, ServiceConstants.KIND_SERVICE);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, primary.getAccountId());
        data.put(ServiceConstants.FIELD_STACK_ID, stackId);

        Map<Long, String> containerIdToLaunchConfigName = new HashMap<>();
        containerIdToLaunchConfigName.put(primary.getId(), name);
        for (Instance sec : secondary) {
            containerIdToLaunchConfigName.put(sec.getId(), sec.getName());
        }

        Map<String, Object> primaryLC = getLaunchConfig(primary, containerIdToLaunchConfigName, true);
        List<Map<String, Object>> secondaryLCs = new ArrayList<>();
        for (Instance sec : secondary) {
            secondaryLCs.add(getLaunchConfig(sec, containerIdToLaunchConfigName, false));
        }

        data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, primaryLC);
        data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, secondaryLCs);
        data.put(ServiceConstants.FIELD_SCALE, 1);
        data.put(ServiceConstants.FIELD_BATCHSIZE, 1);
        data.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, 2000);

        Service service = resourceDao.createAndSchedule(Service.class, data);
        return service;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, Object> getLaunchConfig(Instance instance, Map<Long, String> containerToLCName, boolean isPrimary) {
        Map<String, Object> spec = instanceDao.getInstanceSpec(instance);
        if (isPrimary) {
            spec.remove(ObjectMetaDataManager.NAME_FIELD);
        } else {
            spec.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        }
        if (spec.containsKey(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)) {
            spec.put(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG,
                    containerToLCName.get(Long.valueOf(spec.get(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)
                            .toString())));
            spec.remove(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        }

        Object volumesInstances = spec.get(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        if (volumesInstances != null) {
            List<String> volumesFromLaunchConfigs = new ArrayList<>();
            for (Integer instanceId : (List<Integer>) volumesInstances) {
                volumesFromLaunchConfigs.add(containerToLCName.get(instanceId.longValue()));
            }
            spec.put(ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, volumesFromLaunchConfigs);
            spec.remove(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        }

        return spec;
    }
    
}
