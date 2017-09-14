package io.cattle.iaas.healthcheck.process;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class HealthcheckChangeLock extends AbstractBlockingLockDefintion {

    public HealthcheckChangeLock(Long instanceId) {
        super("healthcheck.change.instance." + instanceId);
    }

}
