package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

public class InstanceImageValidationFilter extends AbstractValidationFilter {

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String imageUuid = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_IMAGE_UUID, String.class);
        if (imageUuid == null) {
            throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, InstanceConstants.FIELD_IMAGE_UUID);
        }

        return super.create(type, request, next);
    }

}