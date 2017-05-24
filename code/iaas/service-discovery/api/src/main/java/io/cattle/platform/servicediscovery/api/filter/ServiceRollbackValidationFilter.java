package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceRollback;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRollbackValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;
    @Inject
    RevisionManager serviceDataMgr;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { ServiceConstants.KIND_SERVICE,
                ServiceConstants.KIND_DNS_SERVICE,
                ServiceConstants.KIND_EXTERNAL_SERVICE,
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE,
                ServiceConstants.KIND_SCALING_GROUP_SERVICE };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
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

        return super.resourceAction(type, request, next);
    }

}