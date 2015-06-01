package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class HealthcheckInstanceLock extends AbstractBlockingLockDefintion {

    public HealthcheckInstanceLock(Long healthcheckInstanceId) {
        super("UPDATE.HCI." + healthcheckInstanceId);
    }

    @Override
    public long getWait() {
        return super.getWait() * 2;
    }
}
