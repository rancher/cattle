package io.cattle.platform.iaas.api.filter.snapshot;

import io.cattle.platform.core.constants.SnapshotConstants;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class SnapshotValidationFilter extends AbstractDefaultResourceManagerFilter {

    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { "snapshot" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Snapshot.class };
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Long volumeId = DataUtils.getFieldFromRequest(request,
                SnapshotConstants.FIELD_VOLUME_ID, Long.class);
        Volume volume = objectManager.loadResource(Volume.class, volumeId);
        if (volume.getImageId() != null) {
            throw new ValidationErrorException(
                    ValidationErrorCodes.INVALID_ACTION,
                    SnapshotConstants.FIELD_VOLUME_ID);
        } else if (!volume.getUri().contains(SnapshotConstants.MANAGED_TAG)) {
            throw new ValidationErrorException(
                    ValidationErrorCodes.INVALID_ACTION,
                    SnapshotConstants.FIELD_VOLUME_ID);
        }
        return super.create(type, request, next);
    }
}
