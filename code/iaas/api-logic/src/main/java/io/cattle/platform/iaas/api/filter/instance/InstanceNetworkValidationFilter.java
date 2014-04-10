package io.cattle.platform.iaas.api.filter.instance;

import java.util.List;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class InstanceNetworkValidationFilter extends AbstractDefaultResourceManagerFilter {

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
        List<?> networkIds = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_NETWORK_IDS, List.class);
        List<?> subnetIds = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_SUBNET_IDS, List.class);

        if ( networkIds != null && subnetIds != null && networkIds.size() > 0 && subnetIds.size() > 0 ) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "NetworkIdsSubnetIdsMutuallyExclusive");
        }

        return super.create(type, request, next);
    }

}
