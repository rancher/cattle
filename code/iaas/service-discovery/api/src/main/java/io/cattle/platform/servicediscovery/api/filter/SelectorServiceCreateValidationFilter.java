package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SelectorServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
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
        Map<String, Object> primaryLaunchConfig = (Map<String, Object>)data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        Object selector = data.get(ServiceConstants.FIELD_SELECTOR_CONTAINER);
        boolean isSelector = selector != null && !selector.toString().isEmpty();
        validateImage(primaryLaunchConfig, isSelector);
    }

    protected void validateImage(Map<String, Object> primaryLaunchConfig, boolean isSelector) {
        if (!isSelector && primaryLaunchConfig != null) {
            Object image = primaryLaunchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
            if (image == null || image.toString().equalsIgnoreCase(ServiceConstants.IMAGE_NONE)) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Image is required when " + ServiceConstants.FIELD_SELECTOR_CONTAINER
                                + " is not passed in");
            }
        }
    }
}
