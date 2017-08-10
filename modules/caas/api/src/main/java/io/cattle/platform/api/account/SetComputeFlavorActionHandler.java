package io.cattle.platform.api.account;

import io.cattle.platform.core.addon.SetComputeFlavorInput;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class SetComputeFlavorActionHandler implements ActionHandler {

    ObjectManager objectManager;

    public SetComputeFlavorActionHandler(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        SetComputeFlavorInput input = request.proxyRequestObject(SetComputeFlavorInput.class);
        objectManager.setFields(obj,
                AccountConstants.FIELD_COMPUTE_FLAVOR, input.getComputeFlavor());
        return obj;
    }

}
