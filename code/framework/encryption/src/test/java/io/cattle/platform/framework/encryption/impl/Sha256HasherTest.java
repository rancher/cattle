package io.cattle.platform.framework.encryption.impl;

import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Sha256HasherTest {


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCompare() throws Exception {
        SecureRandom random = new SecureRandom();
        Sha256Hasher hasher = new Sha256Hasher();
        hasher.init();
        String password;
        for (int i =0; i < 100; i++){
            password = new BigInteger(130, random).toString(32);
            Assert.assertTrue(hasher.compare(password, hasher.transform(password)));
            Assert.assertFalse(hasher.compare("notPassword", hasher.transform(password)));
        }
        password = "lkfdjsafd:fldksaalsd";
        Assert.assertTrue(hasher.compare(password, hasher.transform(password)));
        Assert.assertFalse(hasher.compare("notPassword", hasher.transform(password)));
    }
}
