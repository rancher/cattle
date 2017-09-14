package io.cattle.platform.api.service;

import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

public class VolumeTemplateCreateValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public VolumeTemplateCreateValidationFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

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
