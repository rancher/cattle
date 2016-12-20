package io.github.ibuildthecloud.gdapi.factory.impl;

import io.cattle.platform.util.type.Named;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSchemaFactory implements SchemaFactory, Named {

    @Override
    public String getSchemaName(Class<?> clz) {
        Schema schema = getSchema(clz);
        return schema == null ? null : schema.getId();
    }

    @Override
    public String getSchemaName(String type) {
        Schema schema = getSchema(type);
        return schema == null ? null : schema.getId();
    }

    @Override
    public List<String> getSchemaNames(Class<?> clz) {
        List<String> result = new ArrayList<String>();

        getNames(getSchema(clz), result);

        return result;
    }

    protected void getNames(Schema schema, List<String> result) {
        if (schema == null) {
            return;
        }

        result.add(schema.getId());
        for (String child : schema.getChildren()) {
            Schema childSchema = getSchema(child);
            getNames(childSchema, result);
        }
    }

    @Override
    public Class<?> getSchemaClass(Class<?> type) {
        Schema schema = getSchema(type);
        return schema == null ? null : getSchemaClass(schema.getId());
    }

    @Override
    public String getPluralName(String type) {
        Schema schema = getSchema(type);
        return schema == null ? null : schema.getPluralName();
    }

    @Override
    public String getSingularName(String type) {
        return getSchemaName(type);
    }

    @Override
    public boolean typeStringMatches(Class<?> clz, String type) {
        if (type == null || clz == null)
            return false;

        Schema schema = getSchema(clz);
        if (schema == null)
            return false;

        return schema == getSchema(type);
    }

    @Override
    public Class<?> getSchemaClass(String type, boolean resolveParent) {
        Class<?> clz = getSchemaClass(type);
        if (clz == null && resolveParent) {
            Schema schema = getSchema(type);
            if (schema != null && schema.getParent() != null) {
                return getSchemaClass(schema.getParent(), true);
            }
        }
        return clz;
    }

    @Override
    public String getBaseType(String type) {
        Schema schema = getSchema(type);
        if (schema == null) {
            return null;
        }

        while (schema.getParent() != null) {
            Schema parent = getSchema(schema.getParent());
            if (parent == null) {
                return null;
            }
            schema = parent;
        }

        return schema.getId();
    }

    @Override
    public String getName() {
        return "SchemaFactory:" + getId();
    }

}
