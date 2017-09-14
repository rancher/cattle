package io.cattle.platform.framework.encryption.impl;

import org.junit.Assert;

public class NoOpTransformerTest {

    @org.junit.Test
    public void testEncrypt() throws Exception {
        NoOpTransformer noOpEncrypter = new NoOpTransformer();
        String original = "TestValue";
        String encrypted = noOpEncrypter.transform(original);
        String decrypted = noOpEncrypter.untransform(encrypted);
        Assert.assertEquals(original, decrypted);
    }
}
