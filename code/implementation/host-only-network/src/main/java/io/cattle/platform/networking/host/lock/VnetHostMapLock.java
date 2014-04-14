package io.cattle.platform.networking.host.lock;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class VnetHostMapLock extends AbstractBlockingLockDefintion {

    public VnetHostMapLock(Network network, Host host) {
        super("HOST.ONLY.VNET.MAP." + host.getId());
    }

}
