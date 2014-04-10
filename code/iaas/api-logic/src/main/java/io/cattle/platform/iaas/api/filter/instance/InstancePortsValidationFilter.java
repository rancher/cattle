package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;

public class InstancePortsValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public String[] getTypes() {
        return new String[] { "container", "virtualMachine" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        List<?> ports = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_PORTS, List.class);
        if ( ports != null ) {
            for ( Object port : ports ) {
                if ( port == null ) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, InstanceConstants.FIELD_PORTS);
                }

                /* This will parse the PortSpec and throw an error */
                new PortSpec(port.toString());
            }
        }

        return super.create(type, request, next);
    }

}