package io.cattle.platform.token.impl;

import io.cattle.platform.token.CertSet;

import java.security.PublicKey;
import java.util.Map;

public interface RSAKeyProvider {

    RSAPrivateKeyHolder getPrivateKey();

    Map<String, PublicKey> getPublicKeys();

    PublicKey getDefaultPublicKey();

    CertSet generateCertificate(String subject) throws Exception;

}
