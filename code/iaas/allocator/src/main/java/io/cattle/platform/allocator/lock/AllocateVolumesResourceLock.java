package io.cattle.platform.allocator.lock;

import io.cattle.platform.allocator.service.AllocationRequest.Type;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.lock.definition.AbstractMultiLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.Set;

public class AllocateVolumesResourceLock extends AbstractMultiLockDefinition {

    public AllocateVolumesResourceLock(Set<Volume> volumes) {
        super(getLockDefinitions(volumes));
    }

    protected static LockDefinition[] getLockDefinitions(Set<Volume> volumes) {
        LockDefinition[] result = new LockDefinition[volumes.size()];

        int i = 0;
        for (Volume v : volumes) {
            result[i++] = new AllocateResourceBlockingLock(Type.VOLUME, v.getId());
        }

        return result;
    }
}
