package io.github.ibuildthecloud.dstack.process.virtualmachine;

public interface GenericObjectDataAccess {

    Object setState(Object obj, String fieldName, String state);

    String getState(Object obj, String fieldName);

    Object load(String resourceType, String id);

}
