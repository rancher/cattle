package io.cattle.platform.configitem.server.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

import com.netflix.config.DynamicLongProperty;

public class SyncSourceVersionLock extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty WAIT = ArchaiusUtil.getLong("sync.source.version.lock.wait.millis");

    public SyncSourceVersionLock() {
        super("SYNC.SOURCE.VERSION.LOCK");
    }

    @Override
    public long getWait() {
        return WAIT.get();
    }

}
