package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRestartValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] {
                ServiceConstants.KIND_SERVICE,
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE,
                ServiceConstants.KIND_SCALING_GROUP_SERVICE
                };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
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

        return super.resourceAction(type, request, next);
    }
}
