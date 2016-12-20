package io.cattle.platform.db.jooq.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Schema;
import org.jooq.Table;

public class SchemaRecordTypeListGenerator {

    Class<? extends Schema> schemaClass;

    public List<Class<?>> getRecordTypes() {
        List<Class<?>> result = new ArrayList<Class<?>>();

        Schema schema = null;
        try {
            for (Field field : schemaClass.getFields()) {
                if (field.getType() == schemaClass) {
                    schema = (Schema) field.get(schemaClass);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }

        if (schema == null) {
            throw new IllegalArgumentException("Failed to find TABLE field on [" + schemaClass + "]");
        }

        for (Table<?> table : schema.getTables()) {
            result.add(table.getRecordType());
        }

        return result;
    }

    public Class<? extends Schema> getSchemaClass() {
        return schemaClass;
    }

    public void setSchemaClass(Class<? extends Schema> schemaClass) {
        this.schemaClass = schemaClass;
    }
}
