package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.iaas.healthcheck.service.HealthcheckService.HealthcheckInstanceType;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class HealthcheckRegisterLock extends AbstractBlockingLockDefintion {

    public HealthcheckRegisterLock(Long instanceId, HealthcheckInstanceType instanceType) {
        super("HEALTHCHECK." + instanceId + "." + instanceType.toString() + ".REGISTER");
    }
}
