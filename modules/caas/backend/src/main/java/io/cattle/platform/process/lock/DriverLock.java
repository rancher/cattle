package io.cattle.platform.process.lock;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class DriverLock extends AbstractLockDefinition {

    public DriverLock(Service service, String driverType) {
        super("DRIVER." + service.getId() +  "." + driverType);
    }

}
