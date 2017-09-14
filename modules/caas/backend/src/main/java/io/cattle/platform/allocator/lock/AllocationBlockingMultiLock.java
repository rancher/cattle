package io.cattle.platform.allocator.lock;

import io.cattle.platform.lock.definition.AbstractMultiLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.List;

public class AllocationBlockingMultiLock extends AbstractMultiLockDefinition {

    public AllocationBlockingMultiLock(List<LockDefinition> lockDefinitions) {
        super(lockDefinitions.toArray(new LockDefinition[lockDefinitions.size()]));
    }
}
