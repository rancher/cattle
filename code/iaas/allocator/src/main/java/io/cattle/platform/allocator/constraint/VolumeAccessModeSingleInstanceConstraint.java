package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.VolumeConstants;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;

public class VolumeAccessModeSingleInstanceConstraint extends HardConstraint {

    String volumeName;
    Long volumeId;
    String accessMode;
    List<Long> currentlyUsedBy;
    IdFormatter idFormatter;

    public VolumeAccessModeSingleInstanceConstraint(String volumeName, Long volumeId, String accessMode, List<Long> currentlyUsedBy, IdFormatter idFormatter) {
        this.volumeName = volumeName;
        this.volumeId = volumeId;
        this.accessMode = accessMode;
        this.currentlyUsedBy = currentlyUsedBy;
        this.idFormatter = idFormatter;
    }

    @Override
    public String toString() {
        String vID = idFormatter.formatId(VolumeConstants.TYPE, volumeId).toString();
        return String
                .format("Volume %s(id: %s) has access mode %s and is currently mounted by %s. Cannot be can mounted again.", 
                        volumeName, vID, accessMode, currentlyUsedBy);
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return false;
    }
}
