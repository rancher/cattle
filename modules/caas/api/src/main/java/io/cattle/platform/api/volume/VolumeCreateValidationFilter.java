package io.cattle.platform.api.volume;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class VolumeCreateValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public VolumeCreateValidationFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(VolumeConstants.FIELD_VOLUME_DRIVER) == null &&
                data.get(VolumeConstants.FIELD_STORAGE_DRIVER_ID) == null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.MISSING_REQUIRED,
                    String.format("Either %s or %s is required", VolumeConstants.FIELD_VOLUME_DRIVER,
                            VolumeConstants.FIELD_STORAGE_DRIVER_ID), null);

        }

        Volume volume = request.proxyRequestObject(Volume.class);

        long accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
        Object d = data.get(VolumeConstants.FIELD_VOLUME_DRIVER);
        String driver = d != null ? d.toString() : null;
        Long driverId = volume.getStorageDriverId();
        StorageDriver storageDriver = objectManager.loadResource(StorageDriver.class, driverId);

        if (storageDriver != null) {
            Volume existingVolume = objectManager.findAny(Volume.class,
                    VOLUME.NAME, volume.getName(),
                    VOLUME.REMOVED, null,
                    VOLUME.STORAGE_DRIVER_ID, storageDriver.getId());
            if (existingVolume != null) {
                throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, ObjectMetaDataManager.NAME_FIELD);
            }
        }

        return super.create(type, request, next);
    }
}
