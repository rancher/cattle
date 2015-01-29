package io.cattle.platform.iaas.api.filter.snapshot;

import javax.inject.Inject;

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
        Long volumeId = DataUtils.getFieldFromRequest(request, SnapshotConstants.FIELD_VOLUME_ID, Long.class);
        if ( volumeId == null ) {
        	throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, SnapshotConstants.FIELD_VOLUME_ID);
        }

        Volume volume = objectManager.loadResource(Volume.class, volumeId);
        if ( volume == null ) {
        	throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, SnapshotConstants.FIELD_VOLUME_ID);
        } else {
        	if ( volume.getImageId() != null )  {
        		throw new ValidationErrorException(ValidationErrorCodes.INVALID_ACTION, SnapshotConstants.FIELD_VOLUME_ID);
        	}
        }
        return super.create(type, request, next);
    }
}
