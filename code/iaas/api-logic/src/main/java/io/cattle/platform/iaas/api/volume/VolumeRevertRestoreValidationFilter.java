package io.cattle.platform.iaas.api.volume;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.VolumeConstants.*;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Backup;
import io.cattle.platform.core.model.Snapshot;
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

public class VolumeRevertRestoreValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (!request.getAction().equalsIgnoreCase(ACTION_REVERT) && !request.getAction().equalsIgnoreCase(ACTION_RESTORE)) {
            return super.resourceAction(type, request, next);
        }

        Long volumeId = Long.valueOf(request.getId());
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Long snapshotId = null;
        String field = null;
        String resourceToCheck = null;
        String stateToCheck = null;

        if (request.getAction().equalsIgnoreCase(ACTION_REVERT)) {
            field = "snapshotId";
            snapshotId = (Long)data.get(field);
            resourceToCheck = "Snapshot";

            Snapshot snapshot = objectManager.loadResource(Snapshot.class, snapshotId);
            stateToCheck = snapshot.getState();

            // Ensure the snapshot was made from the volume this action is being performed on
            if (!snapshot.getVolumeId().equals(volumeId)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                        "Snapshot is not for the specified volume.", null);
            }
        } else if (request.getAction().equalsIgnoreCase(ACTION_RESTORE)) {
            field = "backupId";
            Long backupId = (Long)data.get(field);
            Backup backup = objectManager.loadResource(Backup.class, backupId);
            snapshotId = backup.getSnapshotId();
            stateToCheck = backup.getState();
            resourceToCheck = "Backup";

            // If the volume we're restoring isn't the same as the volume from which we created the backup, then make sure neither is a root volume.
            if (!backup.getVolumeId().equals(volumeId)) {
                Volume targetVolume = objectManager.loadResource(Volume.class, volumeId);
                Volume sourceVolume = objectManager.loadResource(Volume.class, backup.getVolumeId());
                if (isRootVolume(targetVolume) || isRootVolume(sourceVolume)) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                            "When restoring a root volume, the backup must have been created from the same volume.", null);
                }
            }
        }

        // Ensure the backup or snapshot is in the created state
        if (!CREATED.equalsIgnoreCase(stateToCheck)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE, resourceToCheck
                    + " must be in created state.", null);
        }

        return super.resourceAction(type, request, next);
    }

    private boolean isRootVolume(Volume volume) {
        // While there is no "is a vm's root volume" flag on a volume, for all practical purposes, if the volume has a base-image
        // driver-opt, it is a vm's root volume and cannot be restored.
        Map<String, Object> opts = DataAccessor.fieldMap(volume, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS);
        return opts.containsKey(VolumeConstants.DRIVER_OPT_BASE_IMAGE);
    }
}
