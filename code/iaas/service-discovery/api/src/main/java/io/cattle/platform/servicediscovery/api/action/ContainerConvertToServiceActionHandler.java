package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class ContainerConvertToServiceActionHandler implements ActionHandler {
    @Inject
    ObjectManager objMgr;
    @Inject
    JsonMapper jsonMapper;

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
        
        if (instance.getContainerServiceId() == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is not qualified for the conversion");
        }
        return updateService(instance);
    }
    
    Service updateService(Instance instance) {
        Service service = objMgr.loadResource(Service.class, instance.getContainerServiceId());
        for (Instance i : objMgr.find(Instance.class, INSTANCE.CONTAINER_SERVICE_ID, service.getId(),
                INSTANCE.REMOVED, null)) {
            objMgr.setFields(i, InstanceConstants.FIELD_SERVICE_ID, service.getId(),
                    InstanceConstants.FIELD_CONTAINER_SERVICE_ID, null);
        }
        objMgr.setFields(service, ObjectMetaDataManager.KIND_FIELD, ServiceConstants.KIND_SERVICE);
        return objMgr.loadResource(Service.class, service.getId());
    }

}
