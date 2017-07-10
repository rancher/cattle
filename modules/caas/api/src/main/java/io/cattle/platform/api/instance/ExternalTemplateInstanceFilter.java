package io.cattle.platform.api.instance;

import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class ExternalTemplateInstanceFilter extends AbstractValidationFilter {

    public static final DynamicStringProperty DEFAULT_REGISTRY = ArchaiusUtil.getString("registry.default");
    public static final DynamicStringListProperty WHITELIST_REGISTRIES = ArchaiusUtil.getList("registry.whitelist");

    StorageService storageService;

    public ExternalTemplateInstanceFilter(StorageService storageService) {
        super();
        this.storageService = storageService;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object imageUuid = data.get(InstanceConstants.FIELD_IMAGE_UUID);

        if (imageUuid != null) {
            String image = imageUuid.toString();
            String fullImageName = getImageUuid(image, storageService);
            if (!StringUtils.equals(image, fullImageName)) {
                data.put(InstanceConstants.FIELD_IMAGE_UUID, fullImageName);
                request.setRequestObject(data);
            }
        }
        return super.create(type, request, next);
    }

    public static String getImageUuid(String image, StorageService storageService) {
        validateImageUuid(image);
        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, InstanceConstants.FIELD_IMAGE_UUID);
        }

        String fullImageName = dockerImage.getFullName();
        String registry = dockerImage.getServer();
        if (!fullImageName.startsWith(registry)) {
            String defaultRegistry = DEFAULT_REGISTRY.get();
            if (image.contains(registry)) {
                fullImageName = registry + "/" + fullImageName;
            }
            else if (!StringUtils.isBlank(defaultRegistry) && !StringUtils.isEmpty(defaultRegistry)) {
                fullImageName = defaultRegistry + "/" + fullImageName;
            }
        }

        if (!storageService.isValidUUID(fullImageName.toString())) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, InstanceConstants.FIELD_IMAGE_UUID);
        }

        return fullImageName;
    }

    protected static void validateImageUuid(String image) {
        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            return;
        }

        String registry = dockerImage.getServer();
        List<String> whitelistRegistries = WHITELIST_REGISTRIES.get();
        String defaultRegistry = DEFAULT_REGISTRY.get();
        String userProvidedRegistry = dockerImage.getServer();

        if (!image.contains(registry)) {
            if(!StringUtils.isBlank(defaultRegistry)) {
                userProvidedRegistry = defaultRegistry;
            }
        }


        if (whitelistRegistries.size() > 0) {
            if (!StringUtils.isEmpty(userProvidedRegistry) && !StringUtils.isBlank(userProvidedRegistry)) {
                if (!whitelistRegistries.contains(userProvidedRegistry)) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                            "The provided registry is not whitelisted", null);
                }
            }
            else {
                if (!StringUtils.isEmpty(defaultRegistry) && !StringUtils.isBlank(defaultRegistry)) {
                    if (!whitelistRegistries.contains(defaultRegistry)) {
                        throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                                "The default registry is not whitelisted", null);
                    }
                }
            }
        }
    }

}
