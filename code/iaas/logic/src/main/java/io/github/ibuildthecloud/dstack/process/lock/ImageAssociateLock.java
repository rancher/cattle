package io.github.ibuildthecloud.dstack.process.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

public class ImageAssociateLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public ImageAssociateLock(Long imageId, Long poolId) {
        super("STAGE." + imageId + "." + poolId);
    }

    @Override
    public long getWait() {
        return 1000;
    }


}
