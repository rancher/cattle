package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import javax.inject.Inject;

public class SelectorServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    AllocatorService allocatorService;
    @Inject
    ObjectManager objManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service" };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);

        validateSelectorOnlyService(request, service);
        return super.create(type, request, next);
    }

    @SuppressWarnings("unchecked")
    protected void validateSelectorOnlyService(ApiRequest request, Service service) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Map<String, Object> primaryLaunchConfig = (Map<String, Object>)data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        if (primaryLaunchConfig == null) {
            return;
        }
        Object selector = data.get(ServiceDiscoveryConstants.FIELD_SELECTOR_CONTAINER);
        boolean isSelector = selector != null && !selector.toString().isEmpty();
        validateImage(primaryLaunchConfig, isSelector);
    }

    protected void validateImage(Map<String, Object> primaryLaunchConfig, boolean isSelector) {
        if (!isSelector) {
            Object image = primaryLaunchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
            if (image == null || image.toString().equalsIgnoreCase(ServiceDiscoveryConstants.IMAGE_NONE)) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Image is required when " + ServiceDiscoveryConstants.FIELD_SELECTOR_CONTAINER
                                + " is not passed in");
            }
        }
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equalsIgnoreCase(ServiceDiscoveryConstants.ACTION_SERVICE_UPGRADE)) {
            Service service = objManager.loadResource(Service.class, request.getId());
            if (ServiceDiscoveryUtil.isNoopService(service, allocatorService)) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_ACTION,
                        "Can't upgrade selector only service");
            }
        }

        return super.resourceAction(type, request, next);
    }
}
