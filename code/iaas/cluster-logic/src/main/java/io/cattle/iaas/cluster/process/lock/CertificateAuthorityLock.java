package io.cattle.iaas.cluster.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class CertificateAuthorityLock extends AbstractBlockingLockDefintion {

    public CertificateAuthorityLock(Long accountId) {
        super("CERT_AUTH." + accountId);
    }

}
