package io.github.ibuildthecloud.dstack.allocator.lock;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest.Type;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractMultiLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

import java.util.Set;

public class AllocateVolumesResourceLock extends AbstractMultiLockDefinition {

    public AllocateVolumesResourceLock(Set<Volume> volumes) {
        super(getLockDefinitions(volumes));
    }

    protected static LockDefinition[] getLockDefinitions(Set<Volume> volumes) {
        LockDefinition[] result = new LockDefinition[volumes.size()];

        int i = 0;
        for ( Volume v : volumes ) {
            result[i++] = new AllocateResourceLock(Type.VOLUME, v.getId());
        }

        return result;
    }
}
