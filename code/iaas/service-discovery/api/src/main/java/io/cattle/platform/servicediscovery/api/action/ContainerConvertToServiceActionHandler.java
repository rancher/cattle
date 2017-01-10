package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.ConvertToServiceInput;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
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
    @Inject
    ServiceDataManager svcDataManager;

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
                    "Container is not qualified for the conversion; instance revision is missing");
        }

        final ConvertToServiceInput input = jsonMapper.convertValue(
                request.getRequestObject(),
                ConvertToServiceInput.class);
        String serviceName = input.getName();
        Long stackId = instance.getStackId();
        if (stackId == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is not qualified for the conversion; stackId is null");
        }
        if (!objMgr.find(Service.class, SERVICE.ACCOUNT_ID, instance.getAccountId(), SERVICE.NAME, serviceName,
                SERVICE.STACK_ID, stackId).isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "Service name " + serviceName + " already exists in the stack");
        }

        return svcDataManager.convertToService(instance, serviceName, stackId);
    }



}
