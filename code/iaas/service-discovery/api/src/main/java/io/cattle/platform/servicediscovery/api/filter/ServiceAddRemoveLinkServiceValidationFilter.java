package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceAddRemoveLinkServiceValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    private enum Operation {
        ADD,
        REMOVE
    }

    private static final Map<String, Operation> ACTIONS;

    static {
        ACTIONS = new HashMap<>();
        ACTIONS.put(ServiceConstants.ACTION_SERVICE_ADD_SERVICE_LINK.toLowerCase(), Operation.ADD);
        ACTIONS.put(ServiceConstants.ACTION_SERVICE_REMOVE_SERVICE_LINK.toLowerCase(), Operation.REMOVE);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (ACTIONS.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            validateAction(Long.valueOf(request.getId()), request.getAction(), data);
        }

        return super.resourceAction(type, request, next);
    }

    private void validateAction(long serviceId, String action, Map<String, Object> data) {
        ServiceLink serviceLink = DataAccessor.fromMap(data).withKey(
                ServiceConstants.FIELD_SERVICE_LINK).as(jsonMapper, ServiceLink.class);
        if (ACTIONS.get(action) == Operation.ADD) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            if (consumeMapDao.findNonRemovedMap(serviceId, serviceLink.getServiceId(), null) != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                        ServiceConstants.FIELD_SERVICE_ID);
            }
            Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
            if (service == null || consumedService == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        ServiceConstants.FIELD_SERVICE_ID);
            }
            validateLinkName(serviceLink.getName());

        } else {
            if (consumeMapDao.findMapToRemove(serviceId, serviceLink.getServiceId()) == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        ServiceConstants.FIELD_SERVICE_ID);
            }
        }
    }
}
