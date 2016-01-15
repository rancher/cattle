package io.cattle.platform.process.common.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

public class MountVolumeLock extends AbstractBlockingLockDefintion implements BlockingLockDefinition {

    public MountVolumeLock(Long volumeId) {
        super("MOUNT.VOLUME." + volumeId);
    }
}
