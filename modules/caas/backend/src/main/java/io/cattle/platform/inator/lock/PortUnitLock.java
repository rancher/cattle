package io.cattle.platform.inator.lock;

import io.cattle.platform.inator.unit.PortUnit;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class PortUnitLock extends AbstractBlockingLockDefintion {

    public PortUnitLock(Long clusterId, PortUnit unit) {
        super("VOLUME.DEFINE." + clusterId + "." + unit.getRef().toString().hashCode());
    }

}