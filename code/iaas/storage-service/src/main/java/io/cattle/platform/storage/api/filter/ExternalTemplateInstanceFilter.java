package io.cattle.platform.storage.api.filter;

import static io.cattle.platform.core.model.tables.ImageTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.storage.ImageCredentialLookup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTemplateInstanceFilter extends AbstractResourceManagerFilter {

    private static final Logger log = LoggerFactory.getLogger(ExternalTemplateInstanceFilter.class);

    @Inject
    ObjectManager objectManager;
    SchemaFactory schemaFactory;
    StorageService storageService;
    ResourceManagerLocator locator;
    List<ImageCredentialLookup> imageCredentialLookups;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object imageUuid = data.get(InstanceConstants.FIELD_IMAGE_UUID);
        Instance instance = request.proxyRequestObject(Instance.class);

        if (imageUuid != null) {
            Image image = validateImageUuid(request.getSchemaFactory(), imageUuid.toString(), instance);
            if (image == null) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, InstanceConstants.FIELD_IMAGE_UUID);
            }
            instance.setImageId(image.getId());
        }

        return super.create(type, request, next);
    }

    protected Image validateImageUuid(SchemaFactory schemaFactory, String uuid, Instance instance) {
        try {
            Image image = storageService.registerRemoteImage(uuid);
            if (image == null) {
                return null;
            }
            Long id = instance.getRegistryCredentialId();
            if (id == null && image.getFormat().equalsIgnoreCase(DockerStoragePoolConstants.DOCKER_FORMAT)) {
                String type = schemaFactory.getSchemaName(StoragePool.class);
                List<?> storagePools = locator.getResourceManagerByType(type).list(type, new HashMap<>(), new ListOptions());
                String typeCredential = schemaFactory.getSchemaName(Credential.class);
                List<?> credentials = locator.getResourceManagerByType(typeCredential).list(typeCredential, new HashMap<>(), new ListOptions());
                for (ImageCredentialLookup imageLookup: imageCredentialLookups){
                    Credential cred = imageLookup.getDefaultCredential(uuid, storagePools, credentials);
                    if (cred == null){
                        continue;
                    }
                    id = cred.getId();
                    if (id != null){
                        break;
                    }
                }
                if (id != null) {
                    instance.setRegistryCredentialId(id);
                }
            }
            if (instance.getRegistryCredentialId() != null) {
                objectManager.setFields(image, IMAGE.REGISTRY_CREDENTIAL_ID, instance.getRegistryCredentialId());
            }

            String type = schemaFactory.getSchemaName(Image.class);
            ResourceManager rm = locator.getResourceManagerByType(type);
            return (Image) rm.getById(type, image.getId().toString(), new ListOptions());
        } catch (IOException e) {
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

    public List<ImageCredentialLookup> imageCredentialLookups() {
        return imageCredentialLookups;
    }

    @Inject
    public void setImageCredentialLookups(List<ImageCredentialLookup> imageCredentialLookups) {
        this.imageCredentialLookups = imageCredentialLookups;
    }
}
