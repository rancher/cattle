package io.cattle.platform.api.instance;

import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.Map;

public class InstanceImageValidationFilter extends AbstractValidationFilter {

    StorageService storageService;

    public InstanceImageValidationFilter(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> fields = CollectionUtils.castMap(request.getRequestObject());
        storageService.validateImageAndSetImage(fields, true);
        return super.create(type, request, next);
    }

}