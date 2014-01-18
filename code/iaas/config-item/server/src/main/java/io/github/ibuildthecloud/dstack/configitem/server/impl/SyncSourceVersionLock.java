package io.github.ibuildthecloud.dstack.configitem.server.impl;

import com.netflix.config.DynamicLongProperty;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

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
