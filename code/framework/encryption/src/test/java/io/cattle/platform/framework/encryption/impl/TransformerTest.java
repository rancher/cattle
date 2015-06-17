package io.cattle.platform.framework.encryption.impl;

import io.github.ibuildthecloud.gdapi.model.Transformer;
import io.github.ibuildthecloud.gdapi.util.TransformationService;
import io.cattle.platform.framework.encryption.handler.impl.TransformationServiceImpl;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

public class TransformerTest {

    private TransformationService transformationService;

    @Before
    public void setUp() throws Exception {
        transformationService = new TransformationServiceImpl();
        Map<String, Transformer> encrypters = new HashMap<>();

        Transformer transformer = new NoOpTransformer();
        transformer.init();
        encrypters.put(transformer.getName(), transformer);
        transformer = new Sha256Hasher();
        transformer.init();
        encrypters.put(transformer.getName(), transformer);
        transformer = new Aes128Encrypter();
        transformer.init();
        encrypters.put(transformer.getName(), transformer);
        transformationService.setTransformers(encrypters);
    }

    @Test
    public void testCompare() throws Exception {
        String toTest;
        byte[] bytes = new byte[22];
        SecureRandom rn = new SecureRandom();

        for (Transformer transformer : transformationService.getTransformers().values()) {
            for (int i = 0; i < 100; i++) {
                rn.nextBytes(bytes);
                toTest = String.valueOf(Hex.encodeHex(bytes));
                String encrypted = transformer.transform(toTest);
                Assert.assertFalse(StringUtils.equals(encrypted, toTest));
                Assert.assertTrue(transformer.compare(toTest, encrypted));
                Assert.assertFalse(transformer.compare("Garbage", encrypted));
            }
        }
    }
}
