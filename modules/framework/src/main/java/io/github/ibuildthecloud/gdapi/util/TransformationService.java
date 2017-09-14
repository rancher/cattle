package io.github.ibuildthecloud.gdapi.util;

public interface TransformationService {

    String transform(String value, String method);

    String untransform(String value);

    boolean compare(String plainText, String encrypted);

}
