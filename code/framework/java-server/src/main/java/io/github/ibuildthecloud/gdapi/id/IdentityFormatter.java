package io.github.ibuildthecloud.gdapi.id;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

public class IdentityFormatter implements IdFormatter {

    @Override
    public Object formatId(String type, Object id) {
        return id;
    }

    @Override
    public String parseId(String id) {
        return id;
    }

    @Override
    public IdFormatter withSchemaFactory(SchemaFactory schemaFactory) {
        return this;
    }

}
