package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.List;

public class VolumeAccessModeSingleInstanceConstraint extends HardConstraint {

    String volumeName;
    String volumeId;
    String accessMode;
    List<Long> currentlyUsedBy;

    public VolumeAccessModeSingleInstanceConstraint(String volumeName, String volumeId, String accessMode, List<Long> currentlyUsedBy) {
        this.volumeName = volumeName;
        this.volumeId = volumeId;
        this.accessMode = accessMode;
        this.currentlyUsedBy = currentlyUsedBy;
    }

    @Override
    public String toString() {
        return String
                .format("Volume %s(id: %s) has access mode %s and is currently mounted by %s. Cannot be can mounted again.", 
                        volumeName, volumeId, accessMode, currentlyUsedBy);
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return false;
    }
}
