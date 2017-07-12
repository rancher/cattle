package io.cattle.platform.object.serialization.impl;

import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;

import java.util.HashMap;
import java.util.Map;

public class ObjectSerializerImpl implements ObjectSerializer {

    IdFormatter idFormatter;
    SchemaFactory schemaFactory;

    public ObjectSerializerImpl(IdFormatter idFormatter, SchemaFactory schemaFactory) {
        this.idFormatter = idFormatter;
        this.schemaFactory = schemaFactory;
    }

    @Override
    public Map<String, Object> serialize(Object input) {
        if (input == null) {
            return new HashMap<>();
        }

        Schema schema = schemaFactory.getSchema(input.getClass());
        if (schema == null) {
            throw new IllegalArgumentException("Failed to find schema for [" + input.getClass() + "]");
        }

        Resource resource = new WrappedResource(idFormatter, schemaFactory, schema, input);
        return CollectionUtils.asMap(schema.getId(), resource);
    }

}
