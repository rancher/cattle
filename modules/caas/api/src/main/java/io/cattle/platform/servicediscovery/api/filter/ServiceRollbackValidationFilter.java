package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceRollback;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

public class ServiceRollbackValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;
    RevisionManager serviceDataMgr;

    public ServiceRollbackValidationFilter(ObjectManager objectManager, RevisionManager serviceDataMgr) {
        super();
        this.objectManager = objectManager;
        this.serviceDataMgr = serviceDataMgr;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request, ActionHandler next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_ROLLBACK)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            Long revisionId = request.proxyRequestObject(ServiceRollback.class).getRevisionId();

            Map<String, Object> data = serviceDataMgr.getServiceDataForRollback(service, revisionId);
            if (data == null) {
                request.setResponseCode(ResponseCodes.NOT_MODIFIED);
                request.setResponseObject(new Object());
                request.commit();
                return request.getResponseObject();
            }

            objectManager.setFields(service, data);
        }

        return super.perform(name, obj, request, next);
    }

}