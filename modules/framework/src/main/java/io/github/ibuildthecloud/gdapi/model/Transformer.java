package io.github.ibuildthecloud.gdapi.model;

public interface Transformer {

    String transform(String value);

    String untransform(String value);

    boolean compare(String plainText, String encrypted);

    String getName();

    void init();

}
