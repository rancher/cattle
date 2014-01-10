package io.github.ibuildthecloud.dstack.object.serialization;

public interface ObjectSerializerFactory {

    ObjectSerializer compile(String type, String expression);

}
