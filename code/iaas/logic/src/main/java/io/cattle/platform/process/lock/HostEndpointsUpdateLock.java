package io.cattle.platform.process.lock;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class HostEndpointsUpdateLock extends AbstractBlockingLockDefintion {

    public HostEndpointsUpdateLock(Host host) {
        super("HOST." + host.getId() + "ENDPOINTS.UPDATE");
    }
}
