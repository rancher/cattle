package io.cattle.platform.storage.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTemplateInstanceFilter extends AbstractResourceManagerFilter {

    private static final Logger log = LoggerFactory.getLogger(ExternalTemplateInstanceFilter.class);

    SchemaFactory schemaFactory;
    StorageService storageService;
    ResourceManagerLocator locator;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String,Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object imageUuid = data.get(InstanceConstants.FIELD_IMAGE_UUID);

        if ( imageUuid != null ) {
            Image image = validateImageUuid(request.getSchemaFactory(), imageUuid.toString());
            if ( image == null ) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, InstanceConstants.FIELD_IMAGE_UUID);
            }

            Instance instance = request.proxyRequestObject(Instance.class);
            instance.setImageId(image.getId());
        }

        return super.create(type, request, next);
    }

    protected Image validateImageUuid(SchemaFactory schemaFactory, String uuid) {
        try {
            Image image = storageService.registerRemoteImage(uuid);
            if ( image == null ) {
                return null;
            }

            String type = schemaFactory.getSchemaName(Image.class);
            ResourceManager rm = locator.getResourceManagerByType(type);

            return (Image)rm.getById(type, image.getId().toString(), new ListOptions());
        } catch ( IOException e ) {
            log.error("Failed to contact external registry", e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "ExternalServiceUnavailable");
        }
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

    public StorageService getStorageService() {
        return storageService;
    }

    @Inject
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

}
