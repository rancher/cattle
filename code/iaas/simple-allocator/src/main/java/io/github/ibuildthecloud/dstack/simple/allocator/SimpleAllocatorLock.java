package io.github.ibuildthecloud.dstack.simple.allocator;

import com.netflix.config.DynamicLongProperty;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

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
