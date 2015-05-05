package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class ServiceLoadBalancerRemoveValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { LoadBalancer.class };
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        LoadBalancer lb = objectManager.loadResource(LoadBalancer.class, id);
        
        if (lb.getServiceId() != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    ServiceDiscoveryConstants.FIELD_SERVICE_ID);
        }

        return super.delete(type, id, request, next);
    }
}
