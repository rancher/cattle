package io.cattle.platform.framework.encryption.handler.impl;


import io.cattle.platform.framework.encryption.EncryptionConstants;
import io.github.ibuildthecloud.gdapi.model.Transformer;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class TransformationServiceImpl implements TransformationService {

    Map<String, Transformer> transformers;

    @Override
    public String transform(String value, String method) {
        String transformer;
        if (StringUtils.isBlank(value)){
            return "";
        }
        if (method.equalsIgnoreCase(EncryptionConstants.ENCRYPT)){
            transformer = EncryptionConstants.ENCRYPTER_NAME.get();
        } else if (method.equalsIgnoreCase(EncryptionConstants.HASH)) {
            transformer = EncryptionConstants.HASHER_NAME.get();
        } else {
            throw new IllegalArgumentException(method);
        }
        transformerExists(transformer);
        return transformer + EncryptionConstants.ENCRYPTER_NAME_DELM + getTransformers().get(transformer).transform(value);
    }

    protected String untransformInternal(String transformerName, String value) {
        transformerExists(transformerName);
        return getTransformers().get(transformerName).untransform(value);
    }

    protected boolean compareInternal(String plainText, String encrypted, String transformerName) {
        transformerExists(transformerName);
        return getTransformers().get(transformerName).compare(plainText, encrypted);
    }

    @Override
    public String untransform(String value) {
        if (StringUtils.isBlank(value)){
            return "";
        }
        String[] valueSplit = value.split(EncryptionConstants.ENCRYPTER_NAME_DELM, 2);
        if (valueSplit.length != 2){
            return value;
        }
        return untransformInternal(valueSplit[0], valueSplit[1]);
    }

    @Override
    public void setTransformers(Map<String, Transformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public Map<String, Transformer> getTransformers() {
        return transformers;
    }

    @Override
    public boolean compare(String plainText, String encrypted) {
        String[] valueSplit = encrypted.split("\\" + EncryptionConstants.ENCRYPTER_NAME_DELM, 2);
        if (valueSplit.length != 2){
            return StringUtils.equals(plainText, encrypted);
        }
        return compareInternal(plainText, valueSplit[1], valueSplit[0]);
    }

    void transformerExists(String transformerName) {
        if (!getTransformers().containsKey(transformerName)){
            throw new IllegalArgumentException("Transformer: " + transformerName + " does not exist.");
        }
    }
}
