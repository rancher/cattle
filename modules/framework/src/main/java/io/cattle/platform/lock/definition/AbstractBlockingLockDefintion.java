package io.cattle.platform.lock.definition;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicLongProperty;

public abstract class AbstractBlockingLockDefintion extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty DEFAULT_WAIT = ArchaiusUtil.getLong("default.lock.wait.millis");

    public AbstractBlockingLockDefintion(String lockId) {
        super(lockId);
    }

    @Override
    public long getWait() {
        return DEFAULT_WAIT.get();
    }

}
