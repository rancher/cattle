package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Volume;

import java.util.HashSet;
import java.util.Set;

public class VolumeValidStoragePoolConstraint extends HardConstraint implements Constraint {

    Volume volume;
    Set<Long> storagePools = new HashSet<Long>();

    public VolumeValidStoragePoolConstraint(Volume volume) {
        super();
        this.volume = volume;
    }

    public Set<Long> getStoragePools() {
        return storagePools;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        Set<Long> poolIds = candidate.getPools().get(volume.getId());

        if (storagePools.size() == 0 && poolIds.size() == 0) {
            return true;
        }

        for (Long poolId : poolIds) {
            if (!storagePools.contains(poolId)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("volume [%d] must be one of pool(s) %s", volume.getId(), storagePools);
    }

}
