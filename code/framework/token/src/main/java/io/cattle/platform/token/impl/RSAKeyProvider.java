package io.cattle.platform.token.impl;

import java.security.PublicKey;
import java.util.Map;

public interface RSAKeyProvider {

    RSAPrivateKeyHolder getPrivateKey();
    
    Map<String, PublicKey> getPublicKeys();
    
    PublicKey getDefaultPublicKey();

}
