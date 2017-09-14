package io.cattle.platform.api.service;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class ServiceRestartValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public ServiceRestartValidationFilter(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request, ActionHandler next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_RESTART)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            Long restartCount = DataAccessor.fieldLong(service, ServiceConstants.FIELD_RESTART_TRIGGER);
            if (restartCount == null) {
                restartCount = 0L;
            }
            restartCount++;
            objectManager.setFields(service,
                    ServiceConstants.FIELD_RESTART_TRIGGER, restartCount);
        }

        return super.perform(obj, request, next);
    }
}
