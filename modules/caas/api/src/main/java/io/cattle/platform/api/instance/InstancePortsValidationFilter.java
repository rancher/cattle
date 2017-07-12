package io.cattle.platform.api.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;

public class InstancePortsValidationFilter extends AbstractValidationFilter {

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        List<?> ports = DataAccessor.getFieldFromRequest(request, InstanceConstants.FIELD_PORTS, List.class);
        if (ports != null) {
            for (Object port : ports) {
                if (port == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, InstanceConstants.FIELD_PORTS);
                }

                /* This will parse the PortSpec and throw an error */
                new PortSpec(port.toString());
            }
        }

        return super.create(type, request, next);
    }

}