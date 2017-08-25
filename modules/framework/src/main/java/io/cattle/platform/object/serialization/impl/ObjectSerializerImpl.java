package io.cattle.platform.object.serialization.impl;

import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ObjectSerializerImpl implements ObjectSerializer {

    IdFormatter idFormatter;
    Map<String, SchemaFactory> factories;
    String[] schemaFactoryNames;

    public ObjectSerializerImpl(IdFormatter idFormatter, Map<String, SchemaFactory> factories, String... schemaFactoryNames) {
        this.idFormatter = idFormatter;
        this.factories = factories;
        this.schemaFactoryNames = schemaFactoryNames;
    }

    @Override
    public Map<String, Object> serialize(Object input) {
        if (input == null) {
            return new HashMap<>();
        }

        SchemaFactory schemaFactory = null;
        for (String name : schemaFactoryNames) {
            schemaFactory = factories.get(name);
            if (schemaFactory != null) {
                break;
            }
        }

        if (schemaFactory == null) {
            throw new IllegalStateException("Failed to find SchemaFactory for " + Arrays.toString(schemaFactoryNames));
        }

        Schema schema = schemaFactory.getSchema(input.getClass());
        if (schema == null) {
            throw new IllegalArgumentException("Failed to find schema for [" + input.getClass() + "]");
        }

        Resource resource = new WrappedResource(idFormatter, schemaFactory, schema, input);
        return CollectionUtils.asMap(schema.getId(), resource);
    }

}
