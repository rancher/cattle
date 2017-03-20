package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRollbackValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ServiceDataManager serviceDataMgr;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "dnsService", "externalService" };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_ROLLBACK)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            final io.cattle.platform.core.addon.ServiceRollback rollback = jsonMapper.convertValue(
                    request.getRequestObject(),
                    io.cattle.platform.core.addon.ServiceRollback.class);
            Map<String, Object> data = serviceDataMgr.getServiceDataForRollback(service, rollback);
            if (service.getState().equalsIgnoreCase(ServiceConstants.STATE_UPGRADED)) {
                data.put(ServiceConstants.FIELD_FINISH_UPGRADE, true);
            }
            objectManager.setFields(objectManager.reload(service), data);
        }

        return super.resourceAction(type, request, next);
    }
    
}