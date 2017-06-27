package io.cattle.platform.framework.encryption.impl;

import io.cattle.platform.framework.encryption.handler.impl.TransformationServiceImpl;
import io.github.ibuildthecloud.gdapi.model.Transformer;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransformerTest {

    private TransformationServiceImpl transformationService;

    static final char[] specialChars = new char[]{
            '\'', '\"', '\\', '/', '[', ']', '*', ':', ';', '%', '$', ',',
            '(', '-', ')', '#', '@', '!', '+'};

    @Before
    public void setUp() throws Exception {
        transformationService = new TransformationServiceImpl();
        Transformer transformer = new NoOpTransformer();
        transformer.init();

        transformationService.addTransformers(transformer);
        transformer = new Sha256Hasher();
        transformer.init();
        transformationService.addTransformers(transformer);
        transformer = new Aes256Encrypter();
        transformer.init();
        transformationService.addTransformers(transformer);
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

        for (Transformer transformer : transformationService.getTransformers().values()) {
            for (int i = 0; i < 100; i++) {
                String password = randomPass(rn);
                String encrypted = transformer.transform(password);
                Assert.assertFalse(StringUtils.equals(encrypted, password));
                Assert.assertTrue(transformer.compare(password, encrypted));
                Assert.assertFalse(transformer.compare("Garbage", encrypted));
            }
        }
    }

    private String randomPass(SecureRandom rn) {
        byte[] bytes = new byte[10];
        rn.nextBytes(bytes);
        char[] rand = String.valueOf(Hex.encodeHex(bytes)).toCharArray();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i <rand.length; i++){
            password.append(specialChars[rn.nextInt(specialChars.length)]);
            password.append(rand[i]);
        }
        return password.toString();
    }
}
