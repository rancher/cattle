package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ServiceAddRemoveLinkServiceValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    private enum Operation {
        ADD,
        REMOVE
    }

    private static final Map<String, Operation> ACTIONS;

    static {
        ACTIONS = new HashMap<>();
        ACTIONS.put(ServiceDiscoveryConstants.ACTION_SERVICE_ADD_SERVICE_LINK.toLowerCase(), Operation.ADD);
        ACTIONS.put(ServiceDiscoveryConstants.ACTION_SERVICE_REMOVE_SERVICE_LINK.toLowerCase(), Operation.REMOVE);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
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
        Long consumedServiceId = (Long) data.get(ServiceDiscoveryConstants.FIELD_SERVICE_ID);
        if (ACTIONS.get(action) == Operation.ADD) {
            if (consumeMapDao.findNonRemovedMap(id, consumedServiceId) != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                        ServiceDiscoveryConstants.FIELD_SERVICE_ID);
            }
        } else {
            if (consumeMapDao.findMapToRemove(id, consumedServiceId) == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        ServiceDiscoveryConstants.FIELD_SERVICE_ID);
            }
        }
    }
}
