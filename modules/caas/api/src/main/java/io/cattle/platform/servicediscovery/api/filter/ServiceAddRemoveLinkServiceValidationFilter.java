package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

public class ServiceAddRemoveLinkServiceValidationFilter extends AbstractValidationFilter {

    ServiceConsumeMapDao consumeMapDao;
    ObjectManager objectManager;
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

    public ServiceAddRemoveLinkServiceValidationFilter(ServiceConsumeMapDao consumeMapDao, ObjectManager objectManager, JsonMapper jsonMapper) {
        super();
        this.consumeMapDao = consumeMapDao;
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request, ActionHandler next) {
        if (ACTIONS.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            validateAction(Long.valueOf(request.getId()), request.getAction(), data);
        }

        return super.perform(name, obj, request, next);
    }

    private void validateAction(long serviceId, String action, Map<String, Object> data) {
        ServiceLink serviceLink = DataAccessor.fromMap(data).withKey(
                ServiceConstants.FIELD_SERVICE_LINK).as(ServiceLink.class);
        if (ACTIONS.get(action) == Operation.ADD) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            if (consumeMapDao.findNonRemovedMap(serviceId, serviceLink.getServiceId(), null) != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                        InstanceConstants.FIELD_SERVICE_ID);
            }
            Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
            if (service == null || consumedService == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        InstanceConstants.FIELD_SERVICE_ID);
            }
            ServiceUtil.validateLinkName(serviceLink.getName());

        } else {
            if (consumeMapDao.findMapToRemove(serviceId, serviceLink.getServiceId()) == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        InstanceConstants.FIELD_SERVICE_ID);
            }
        }
    }
}
