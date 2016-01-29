package io.cattle.platform.iaas.api.auth.dynamic;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSchemaFactory extends AbstractSchemaFactory implements SchemaFactory {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaFactory.class);

    long accountId;
    SchemaFactory factory;
    DynamicSchemaDao dynamicSchemaDao;
    JsonMapper jsonMapper;
    String role;

    public DynamicSchemaFactory(long accountId, SchemaFactory factory, DynamicSchemaDao dynamicSchemaDao, JsonMapper jsonMapper, String role) {
        this.accountId = accountId;
        this.factory = factory;
        this.dynamicSchemaDao = dynamicSchemaDao;
        this.jsonMapper = jsonMapper;
        this.role = role;
    }

    @Override
    public String getId() {
        return factory.getId();
    }

    @Override
    public List<Schema> listSchemas() {
        List<Schema> base = factory.listSchemas();
        List<? extends DynamicSchema> dynamic = dynamicSchemaDao.getSchemas(accountId, role);

        List<Schema> result = new ArrayList<>(base.size() + dynamic.size());
        result.addAll(base);

        for (DynamicSchema dynamicSchema : dynamic) {
            Schema schema = safeConvert(dynamicSchema);
            if (schema != null) {
                result.add(schema);
            }
        }

        return result;
    }

    @Override
    public Schema getSchema(Class<?> clz) {
        return factory.getSchema(clz);
    }

    @Override
    public Schema getSchema(String type) {
        if (type == null) {
            return null;
        }
        Schema schema = factory.getSchema(type);
        if (schema != null) {
            return schema;
        }
        DynamicSchema dynamicSchema = dynamicSchemaDao.getSchema(type, accountId, role);
        return safeConvert(dynamicSchema);
    }

    protected Schema safeConvert(DynamicSchema dynamicSchema) {
        try {
            return convert(dynamicSchema);
        } catch (IOException e) {
            log.error("Failed to construct dynamic schema for [{}]", dynamicSchema.getId(), e);
            return null;
        }
    }

    private Schema convert(DynamicSchema dynamicSchema) throws IOException {
        if (dynamicSchema == null || dynamicSchema.getDefinition() == null) {
            return null;
        }

        if (dynamicSchema.getParent() == null) {
            SchemaImpl newSchema = jsonMapper.readValue(dynamicSchema.getDefinition().getBytes("UTF-8"), SchemaImpl.class);
            newSchema.setId(dynamicSchema.getName());
            return newSchema;
        }

        Schema parentSchema = factory.getSchema(dynamicSchema.getParent());
        if (parentSchema == null || !(parentSchema instanceof SchemaImpl)) {
            return null;
        }

        SchemaImpl newSchema = jsonMapper.readValue(dynamicSchema.getDefinition().getBytes("UTF-8"), SchemaImpl.class);
        newSchema.setName(dynamicSchema.getName());
        SchemaImpl mergedSchema = new SchemaImpl((SchemaImpl)parentSchema);
        mergedSchema.getResourceFields().putAll(newSchema.getResourceFields());
        mergedSchema.setPluralName(newSchema.getPluralName());
        mergedSchema.setId(dynamicSchema.getName());
        mergedSchema.setParent(dynamicSchema.getParent());
        mergedSchema.setCollectionMethods(newSchema.getCollectionMethods());
        mergedSchema.setResourceMethods(newSchema.getResourceMethods());
        return mergedSchema;
    }

    @Override
    public Class<?> getSchemaClass(String type) {
        Class<?> clz = factory.getSchemaClass(type);
        if (clz != null) {
            return clz;
        }

        String baseType = getBaseType(type);
        return factory.getSchemaClass(baseType);
    }

    @Override
    public Schema registerSchema(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Schema parseSchema(String name) {
        throw new UnsupportedOperationException();
    }
}
