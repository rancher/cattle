package io.cattle.platform.iaas.api.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import javax.inject.Inject;

public class VolumeCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
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

        if (volume.getSizeMb() != null) {
            // TODOCAJ Once drivers are merged, update this filter to check to see if the driver supports size.
            String driver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
            if (driver != null && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                        String.format("Driver %s does not support size property.", driver), null);

            }
        }
        
        return super.create(type, request, next);
    }
}
