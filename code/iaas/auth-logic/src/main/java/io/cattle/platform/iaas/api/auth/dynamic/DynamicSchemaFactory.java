package io.cattle.platform.iaas.api.auth.dynamic;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

public class DynamicSchemaFactory extends AbstractSchemaFactory implements SchemaFactory {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaFactory.class);

    Cache<Pair<Long, String>, List<Schema>> schemasListCache;
    Cache<DynamicSchemaDao.CacheKey, Schema> schemaCache;
    long accountId;
    SchemaFactory factory;
    DynamicSchemaDao dynamicSchemaDao;
    JsonMapper jsonMapper;
    String role;

    public DynamicSchemaFactory(long accountId, SchemaFactory factory, DynamicSchemaDao dynamicSchemaDao, JsonMapper jsonMapper, String role,
            Cache<Pair<Long, String>, List<Schema>> schemasListCache, Cache<DynamicSchemaDao.CacheKey, Schema> schemaCache) {
        this.schemaCache = schemaCache;
        this.schemasListCache = schemasListCache;
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
        try {
            return this.schemasListCache.get(Pair.of(accountId, role), new Callable<List<Schema>>() {
                @Override
                public List<Schema> call() throws Exception {
                    Map<String, Schema> schemas = new TreeMap<String, Schema>();
                    for (Schema s : factory.listSchemas()) {
                        schemas.put(s.getId(), s);
                    }

                    List<? extends DynamicSchema> dynamic = dynamicSchemaDao.getSchemas(accountId, role);

                    for (DynamicSchema dynamicSchema : dynamic) {
                        Schema schema = convert(dynamicSchema);
                        if (schema != null) {
                            schemas.put(schema.getId(), schema);
                        }
                    }

                    List<Schema> result = new ArrayList<>(schemas.size());
                    result.addAll(schemas.values());
                    return result;
                }
            });
        } catch (ExecutionException e) {
            log.error("Failed to construct dynamic schema list for [{}]", accountId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Schema getSchema(Class<?> clz) {
        return factory.getSchema(clz);
    }

    @Override
    public Schema getSchema(final String type) {
        if (type == null) {
            return null;
        }
        if (type.contains("host") || type.contains("machine") || type.toLowerCase().contains("config")) {
            final DynamicSchema dynamicSchema = dynamicSchemaDao.getSchema(type, accountId, role);
            if (dynamicSchema != null) {
                try {
                    return schemaCache.get(new DynamicSchemaDao.CacheKey(type, accountId, role), new Callable<Schema>() {
                        @Override
                        public Schema call() throws Exception {
                            return convert(dynamicSchema);
                        }
                    });
                } catch (ExecutionException e) {
                    log.error("Failed to construct dynamic schema for [{}]", dynamicSchema.getId(), e);
                    return null;
                }
            }
        }
        return factory.getSchema(type);
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
        mergedSchema.setPluralName(newSchema.getPluralName());
        mergedSchema.setId(dynamicSchema.getName());
        mergedSchema.setParent(dynamicSchema.getParent());
        mergedSchema.setCollectionMethods(newSchema.getCollectionMethods());
        mergedSchema.setResourceMethods(newSchema.getResourceMethods());

        if (mergedSchema.getParent().equals(mergedSchema.getId())) {
            mergedSchema.setParent(parentSchema.getParent());
        }

        Map<String, Field> existingFields = mergedSchema.getResourceFields();
        for (Map.Entry<String, Field> entry : newSchema.getResourceFields().entrySet()) {
            Field oldField = existingFields.put(entry.getKey(), entry.getValue());
            if (oldField instanceof FieldImpl) {
                ((FieldImpl)entry.getValue()).setReadMethod(((FieldImpl) oldField).getReadMethod());
            }
        }

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
