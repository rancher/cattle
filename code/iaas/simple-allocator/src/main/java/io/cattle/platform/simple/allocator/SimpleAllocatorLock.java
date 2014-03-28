package io.cattle.platform.simple.allocator;

import com.netflix.config.DynamicLongProperty;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

public class SimpleAllocatorLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public static final DynamicLongProperty WAIT = ArchaiusUtil.getLong("simple.allocator.lock.wait");

    public SimpleAllocatorLock() {
        super("SIMPLE.ALLOCATOR.LOCK");
    }

    @Override
    public long getWait() {
        return WAIT.get();
    }

}
