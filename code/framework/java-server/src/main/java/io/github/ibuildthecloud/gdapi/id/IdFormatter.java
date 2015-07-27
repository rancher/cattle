package io.github.ibuildthecloud.gdapi.id;

public interface IdFormatter {
    Object formatId(String type, Object id);

    String parseId(String id);
}
