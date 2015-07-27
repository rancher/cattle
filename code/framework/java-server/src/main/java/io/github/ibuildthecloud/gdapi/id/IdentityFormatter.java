package io.github.ibuildthecloud.gdapi.id;

public class IdentityFormatter implements IdFormatter {

    @Override
    public Object formatId(String type, Object id) {
        return id;
    }

    @Override
    public String parseId(String id) {
        return id;
    }

}
