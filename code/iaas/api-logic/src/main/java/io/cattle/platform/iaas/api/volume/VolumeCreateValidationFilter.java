package io.cattle.platform.iaas.api.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class VolumeCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "volume" };
    }

    @Inject
    ObjectManager objectManager;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Volume volume = request.proxyRequestObject(Volume.class);

        if (volume.getSizeMb() != null) {
            // TODO Once drivers are merged, update this filter to check to see if the driver supports size.
            String driver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
            if (!VolumeConstants.LOCAL_DRIVER.equals(driver)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                        "Driver %s does not support size property.", null);

            }
        }

        return super.create(type, request, next);
    }
}
