package io.cattle.platform.api.instance;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.revision.RevisionManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import static io.cattle.platform.core.model.tables.ServiceTable.*;


public class ContainerConvertToServiceActionHandler implements ActionHandler {

    ObjectManager objMgr;
    JsonMapper jsonMapper;
    RevisionManager svcDataManager;

    public ContainerConvertToServiceActionHandler(ObjectManager objMgr, JsonMapper jsonMapper, RevisionManager svcDataManager) {
        this.objMgr = objMgr;
        this.jsonMapper = jsonMapper;
        this.svcDataManager = svcDataManager;
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

        if (instance.getDeploymentUnitId() == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is not qualified for the conversion; legacy or native container");
        }

        Long stackId = instance.getStackId();
        if (stackId == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Container is not qualified for the conversion; stackId is null");
        }
        if (!objMgr.find(Service.class, SERVICE.ACCOUNT_ID, instance.getAccountId(), SERVICE.NAME, instance.getName(),
                SERVICE.STACK_ID, stackId).isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "Service name " + instance.getName() + " already exists in the stack");
        }

        return svcDataManager.convertToService(instance);
    }



}
