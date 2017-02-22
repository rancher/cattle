package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class InstanceAgentValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] {InstanceConstants.TYPE_CONTAINER};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        Object instance = objectManager.loadResource(type, id);
        if (instance == null || !(instance instanceof Instance)) {
            return super.delete(type, id, request, next);
        }

        if (InstanceConstants.isRancherAgent((Instance)instance)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.ACTION_NOT_AVAILABLE,
                    "Can not delete rancher-agent", null);
        }

        return super.delete(type, id, request, next);
    }

}