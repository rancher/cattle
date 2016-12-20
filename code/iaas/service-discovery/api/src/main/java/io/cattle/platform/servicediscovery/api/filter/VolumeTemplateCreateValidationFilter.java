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
import javax.inject.Named;

@Named
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

        validateNameUniqueness(template);
        validateScope(template);

        return super.create(type, request, next);
    }

    private void validateNameUniqueness(VolumeTemplate template) {
        if (objectManager.find(VolumeTemplate.class, VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.STACK_ID,
                template.getStackId(), VOLUME_TEMPLATE.NAME, template.getName()).size() > 0) {
            throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, "name");
        }
    }

    private void validateScope(VolumeTemplate template) {
        if (!(Boolean.TRUE.equals(template.getPerContainer()) || Boolean.TRUE.equals(template.getExternal()) || template
                .getStackId() != null)) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                    "Scope is not set on the volume. Should either be per container, external or per stack");
        }
    }
}
