package io.cattle.platform.framework.encryption.handler.impl;

import io.github.ibuildthecloud.gdapi.model.Transformer;
import io.cattle.platform.framework.encryption.impl.Aes256Encrypter;
import io.cattle.platform.framework.encryption.impl.NoOpTransformer;
import io.cattle.platform.framework.encryption.impl.Sha256Hasher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TransformationServiceImplTest {

    private TransformationServiceImpl encrypterHandler;

    @Before
    public void setUp() throws Exception {
        encrypterHandler = new TransformationServiceImpl();
        Map<String, Transformer> transformers = new HashMap<>();
        Transformer transformer = new NoOpTransformer();
        transformers.put(transformer.getName(), transformer);
        transformer = new Sha256Hasher();
        transformer.init();
        transformers.put(transformer.getName(), transformer);
        transformer = new Aes256Encrypter();
        transformer.init();
        transformers.put(transformer.getName(), transformer);
        encrypterHandler.setTransformers(transformers);
    }

    @Test
    public void testEncryptDecryptValidEncrypter() throws Exception {
        String value = "somevalue";
        String encrypted = encrypterHandler.transform(value, "hash");
        Assert.assertTrue(encrypterHandler.compare(value, encrypted));
        encrypted = encrypterHandler.transform(value, "encrypt");
        Assert.assertTrue(encrypterHandler.compare(value, encrypted));
    }

    @Test
    public void testEncryptDecryptInValid() throws Exception {
        String value = "Some value";
        String hashed = encrypterHandler.transform(value, "hash");
        Assert.assertFalse(encrypterHandler.compare("sdffdsa", hashed));
        hashed = encrypterHandler.transform(value, "encrypt");
        Assert.assertFalse(encrypterHandler.compare("sdffdsa", hashed));
    }

    @Test
    public void testNullEmptyInput() throws Exception {
        Assert.assertTrue(encrypterHandler.transform("", "encrypt").equals(""));
        Assert.assertTrue(encrypterHandler.transform(null, "encrypt").equals(""));
        Assert.assertTrue(encrypterHandler.transform("", "hash").equals(""));
        Assert.assertTrue(encrypterHandler.transform(null, "hash").equals(""));
        Assert.assertTrue(encrypterHandler.untransform("").equals(""));
        Assert.assertTrue(encrypterHandler.untransform(null).equals(""));
    }
}
