package io.github.ibuildthecloud.gdapi.util;

import io.github.ibuildthecloud.gdapi.model.Transformer;

import java.util.Map;

public interface TransformationService {

    String transform(String value, String method);

    String untransform(String value);

    boolean compare(String plainText, String encrypted);

    void setTransformers(Map<String, Transformer> encrypters);

    Map<String, Transformer> getTransformers();
}
