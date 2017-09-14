package io.cattle.platform.token.impl;

import io.cattle.platform.token.CertSet;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Map;

public interface RSAKeyProvider {

    RSAPrivateKeyHolder getPrivateKey();

    Map<String, PublicKey> getPublicKeys();

    PublicKey getDefaultPublicKey();

    CertSet generateCertificate(String subject, String... sans) throws Exception;

    Certificate getCACertificate();

    byte[] toBytes(Certificate cert) throws IOException;

}
