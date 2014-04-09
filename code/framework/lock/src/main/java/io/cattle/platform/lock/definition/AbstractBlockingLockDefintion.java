package io.cattle.platform.lock.definition;

import com.netflix.config.DynamicLongProperty;

import io.cattle.platform.archaius.util.ArchaiusUtil;

public abstract class AbstractBlockingLockDefintion extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty DEFAULT_WAIT = ArchaiusUtil.getLong("default.lock.wait.millis");

    public AbstractBlockingLockDefintion(String lockId) {
        super(lockId);
    }

    @Override
    public long getWait() {
        String lockId = getLockId();
        if ( lockId == null ) {
            return DEFAULT_WAIT.get();
        } else {
            long wait = ArchaiusUtil.getLong(lockId + ".wait").get();
            return wait == 0 ? DEFAULT_WAIT.get() : wait;
        }
    }

}
