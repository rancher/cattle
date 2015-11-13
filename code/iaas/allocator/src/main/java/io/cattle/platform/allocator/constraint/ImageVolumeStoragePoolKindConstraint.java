package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ImageVolumeStoragePoolKindConstraint extends HardConstraint implements Constraint {

    private static final Set<String> VALID_POOL_KINDS = new HashSet<String>(Arrays.asList("sim", "docker"));

    private Volume volume;

    public ImageVolumeStoragePoolKindConstraint(Volume volume) {
        super();
        this.volume = volume;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        Set<Long> poolIds = candidate.getPools().get(volume.getId());
        for (Long id : poolIds) {
            StoragePool pool = candidate.loadResource(StoragePool.class, id);
            if (!VALID_POOL_KINDS.contains(pool.getKind())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("storage pool for volume %s must be one of kind %s", volume.getId(), VALID_POOL_KINDS);
    }
}
