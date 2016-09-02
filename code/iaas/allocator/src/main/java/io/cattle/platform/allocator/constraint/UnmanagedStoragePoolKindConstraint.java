package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.Set;

public class UnmanagedStoragePoolKindConstraint extends HardConstraint implements Constraint {

    private Volume volume;

    public UnmanagedStoragePoolKindConstraint(Volume volume) {
        super();
        this.volume = volume;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        Set<Long> poolIds = candidate.getPools().get(volume.getId());
        for (Long id : poolIds) {
            StoragePool pool = candidate.loadResource(StoragePool.class, id);
            if (!AllocatorUtils.UNMANGED_STORAGE_POOLS.contains(pool.getKind())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("storage pool for volume %s must be one of kind %s", volume.getId(), AllocatorUtils.UNMANGED_STORAGE_POOLS);
    }
}
