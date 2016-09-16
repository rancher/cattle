package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.List;

public class VolumeAccessModeSingleInstanceConstraint extends HardConstraint {

    Long volumeId;
    String accessMode;
    List<Long> currentlyUsedBy;

    public VolumeAccessModeSingleInstanceConstraint(Long volumeId, String accessMode, List<Long> currentlyUsedBy) {
        this.volumeId = volumeId;
        this.accessMode = accessMode;
        this.currentlyUsedBy = currentlyUsedBy;
    }

    @Override
    public String toString() {
        return String
                .format("Volume %s has access mode %s and is currently mounted by %s. Cannot be can mounted again.", volumeId, accessMode, currentlyUsedBy);
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return false;
    }
}
