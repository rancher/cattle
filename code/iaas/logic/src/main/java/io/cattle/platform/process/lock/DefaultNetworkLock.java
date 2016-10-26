package io.cattle.platform.process.lock;

import io.cattle.platform.core.model.Network;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DefaultNetworkLock extends AbstractBlockingLockDefintion {

    public DefaultNetworkLock(Network network) {
        super("DEFAULT.NETWORK." + network.getAccountId());
    }

}
