package io.cattle.platform.process.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

public class ImageAssociateLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public ImageAssociateLock(Long imageId, Long poolId) {
        super("STAGE." + imageId + "." + poolId);
    }

    @Override
    public long getWait() {
        return 1000;
    }

}
