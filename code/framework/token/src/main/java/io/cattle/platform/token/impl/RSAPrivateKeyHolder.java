package io.cattle.platform.token.impl;

import java.security.interfaces.RSAPrivateKey;

public class RSAPrivateKeyHolder {

    String keyId;
    RSAPrivateKey key;

    public RSAPrivateKeyHolder(String keyId, RSAPrivateKey key) {
        super();
        this.keyId = keyId;
        this.key = key;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public RSAPrivateKey getKey() {
        return key;
    }

    public void setKey(RSAPrivateKey key) {
        this.key = key;
    }

}
