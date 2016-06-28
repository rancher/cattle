package io.cattle.platform.iaas.api.volume;

import static io.cattle.platform.core.constants.VolumeConstants.*;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class VolumeRevertRestoreValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final Set<String> ALLOWED_STATES;
    static {
        ALLOWED_STATES = new HashSet<String>();
        ALLOWED_STATES.add("snapshotted");
        ALLOWED_STATES.add("backedup");
        ALLOWED_STATES.add("backedup-only");
    }

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (!request.getAction().equalsIgnoreCase(ACTION_REVERT)) {
            return super.resourceAction(type, request, next);
        }
        
        // TODO This class had some of the biggest changes and lost a little functionality (cross-volume restore)
        Long volumeId = Long.valueOf(request.getId());
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Long snapshotId = (Long)data.get("snapshotId");
        Snapshot snapshot = objectManager.loadResource(Snapshot.class, snapshotId);

        // Ensure the snapshot was made from the volume this action is being performed on
        if (!snapshot.getVolumeId().equals(volumeId)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    "Snapshot is not for the specified volume.", null);
        }

        // Ensure the snapshot is in an appropriate state
        if (!ALLOWED_STATES.contains(snapshot.getState())) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    String.format("Snapshot must be in one of states: %s.", ALLOWED_STATES), null);
        }

        return super.resourceAction(type, request, next);
    }
}
