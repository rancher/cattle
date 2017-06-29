package io.cattle.platform.object.serialization;

public interface ObjectSerializerFactory {

    ObjectSerializer compile(String type, String expression);

}
