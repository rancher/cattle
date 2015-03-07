package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerHostValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    GenericMapDao mapDao;

    private static final Map<String, Boolean> ACTIONS;

    static {
        ACTIONS = new HashMap<>();
        ACTIONS.put(LoadBalancerConstants.ACTION_ADD_HOST.toLowerCase(), true);
        ACTIONS.put(LoadBalancerConstants.ACTION_REMOVE_HOST.toLowerCase(), false);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { LoadBalancer.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (ACTIONS.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            validateAction(Long.valueOf(request.getId()), data, request.getAction());
        }

        return super.resourceAction(type, request, next);
    }

    private void validateAction(long id, Map<String, Object> data, String action) {
        Long hostId = (Long) data.get(LoadBalancerConstants.FIELD_LB_HOST_ID);

        if (ACTIONS.get(action)) {
            if (mapDao.findNonRemoved(LoadBalancerHostMap.class, Host.class, hostId, LoadBalancer.class, id) != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, LoadBalancerConstants.FIELD_LB_HOST_ID);
            }
        } else {
            if (mapDao.findToRemove(LoadBalancerHostMap.class, Host.class, hostId, LoadBalancer.class, id) == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION, LoadBalancerConstants.FIELD_LB_HOST_ID);
            }
        }
    }
}
