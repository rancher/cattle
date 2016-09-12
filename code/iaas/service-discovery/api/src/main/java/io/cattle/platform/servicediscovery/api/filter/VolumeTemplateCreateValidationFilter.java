package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class VolumeTemplateCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { VolumeTemplate.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "volumeTemplate" };
    }

    @Inject
    ObjectManager objectManager;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        VolumeTemplate template = request.proxyRequestObject(VolumeTemplate.class);
        
        if (objectManager.find(VolumeTemplate.class, VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.STACK_ID,
                template.getStackId(), VOLUME_TEMPLATE.NAME, template.getName()).size() > 0) {
            throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, "name");
        }
        return super.create(type, request, next);
    }
}
