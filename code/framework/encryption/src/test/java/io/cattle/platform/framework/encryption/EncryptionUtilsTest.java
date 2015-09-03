package io.cattle.platform.framework.encryption;

import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;


public class EncryptionUtilsTest {

    @Test
    public void testGetKeyFromFile() throws Exception {
        byte[] bytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        EncryptionUtils.saveKeyToFile("James", bytes);
        byte[] gotten = EncryptionUtils.getKeyFromFile("James");
        Assert.assertArrayEquals(bytes, gotten);
        for (int i = 0; i < 10; i++){
            String key = new BigInteger(130, random).toString(32);
            random.nextBytes(bytes);
            EncryptionUtils.saveKeyToFile(key, bytes);
            Assert.assertArrayEquals(EncryptionUtils.getKeyFromFile(key), bytes);
        }
    }
}
