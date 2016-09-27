package io.cattle.platform.docker.machine.api.filter;

import static io.cattle.platform.core.constants.MachineConstants.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class MachineValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final String DRIVER_CONFIG_EXACTLY_ONE_REQUIRED = "DriverConfigExactlyOneRequired";

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {Host.class};
    }

    @Override
    public String[] getTypes() {
        return new String[] { KIND_MACHINE };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        // Don't validate hosts for v1 API
        if (HostConstants.TYPE.equals(type) && ("v1".equals(request.getVersion()) ||
                AccountConstants.SUPER_ADMIN_KIND.equals(request.getSchemaFactory().getId()))) {
            return super.create(type, request, next);
        }

        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        boolean alreadyFound = false;

        for (Map.Entry<String, Object> field : data.entrySet()) {
            if (StringUtils.endsWithIgnoreCase(field.getKey(), CONFIG_FIELD_SUFFIX) && field.getValue() != null) {
                if (alreadyFound) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
                }
                alreadyFound = true;
            }
        }

        if (!alreadyFound) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
        }
        return super.create(type, request, next);
    }
}
