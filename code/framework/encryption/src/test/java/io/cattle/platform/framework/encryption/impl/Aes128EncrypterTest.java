package io.cattle.platform.framework.encryption.impl;

import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;

import java.security.SecureRandom;

public class Aes128EncrypterTest {

    @org.junit.Test
    public void testEncrypt() throws Exception {
        Aes128Encrypter encrypter = new Aes128Encrypter();
        encrypter.init();
        String value;
        byte[] bytes = new byte[22];
        SecureRandom rn = new SecureRandom();
        for (int i = 0; i < 100; i++) {
            rn.nextBytes(bytes);
            value = String.valueOf(Hex.encodeHex(bytes));
            String encrypted = encrypter.encrypt(value);
            Assert.assertTrue(encrypter.compare(value,encrypted));
            Assert.assertFalse(encrypter.compare("garbage",encrypted));
        }
    }
}
