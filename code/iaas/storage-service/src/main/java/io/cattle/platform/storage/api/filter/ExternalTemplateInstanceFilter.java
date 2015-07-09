package io.cattle.platform.storage.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ExternalTemplateInstanceFilter extends AbstractResourceManagerFilter {

    SchemaFactory schemaFactory;
    @Inject
    StorageService storageService;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object imageUuid = data.get(InstanceConstants.FIELD_IMAGE_UUID);

        if (imageUuid != null) {
            if (!storageService.isValidUUID(imageUuid.toString())) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, InstanceConstants.FIELD_IMAGE_UUID);
            }
        }
        return super.create(type, request, next);
    }

    @Override
    public String[] getTypes() {
        List<String> types = schemaFactory.getSchemaNames(Instance.class);
        return types.toArray(new String[types.size()]);
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }
}
