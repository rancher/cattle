package io.cattle.platform.inator.lock;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.inator.unit.PortUnit;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class PortUnitLock extends AbstractBlockingLockDefintion {

    public PortUnitLock(Account account, PortUnit unit) {
        super("VOLUME.DEFINE." + account.getId() + "." + unit.getRef().toString().hashCode());
    }

}