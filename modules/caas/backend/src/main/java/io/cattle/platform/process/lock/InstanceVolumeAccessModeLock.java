package io.cattle.platform.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class InstanceVolumeAccessModeLock extends AbstractBlockingLockDefintion {

    public InstanceVolumeAccessModeLock(Long volumeId) {
        super("INSTANCE.VOLUME.ACCESSMODE." + volumeId);
    }
}
