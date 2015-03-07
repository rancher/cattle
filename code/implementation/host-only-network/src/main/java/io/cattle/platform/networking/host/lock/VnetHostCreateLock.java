package io.cattle.platform.networking.host.lock;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class VnetHostCreateLock extends AbstractBlockingLockDefintion {

    public VnetHostCreateLock(Network network, Host host) {
        super("HOST.ONLY.VNET.CREATE." + network.getId() + "." + (host.getPhysicalHostId() == null ? host.getId() : host.getPhysicalHostId()));
    }

}
