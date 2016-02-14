package io.github.ibuildthecloud.gdapi.id;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

public interface IdFormatter {
    Object formatId(String type, Object id);

    String parseId(String id);

    IdFormatter withSchemaFactory(SchemaFactory schemaFactory);
}
