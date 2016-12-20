package io.cattle.platform.api.schema;

import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SubSchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

public class FileSchemaFactory extends AbstractSchemaFactory implements InitializationTask {

    @Inject
    JsonMapper jsonMapper;
    @Inject @Named("CoreSchemaFactory")
    SchemaFactory schemaFactory;
    String file, id;
    Map<String, Schema> schemaMap = new TreeMap<>();
    Map<String, Class<?>> schemaClasses = new HashMap<>();
    List<Schema> schemas = new ArrayList<>();
    boolean init;

    @Override
    public synchronized void start() {
        if (init) {
            return;
        }

        if (schemaFactory instanceof SubSchemaFactory) {
            ((SubSchemaFactory) schemaFactory).init();
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try(InputStream is = cl.getResourceAsStream(file)) {
            ObjectInputStream ois = new ObjectInputStream(is);
            @SuppressWarnings("unchecked")
            List<Schema> serializedSchemas = (List<Schema>) ois.readObject();
            for (Schema schema : serializedSchemas) {
                ((SchemaImpl) schema).setType("schema");
                schema.getActions().clear();
                schema.getLinks().clear();
                copyAccessors(schema);
                schemaMap.put(schema.getId().toLowerCase(), schema);
                if (StringUtils.isNotBlank(schema.getPluralName())) {
                    schemaMap.put(schema.getPluralName().toLowerCase(), schema);
                }
                schemas.add(schema);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        init = true;
    }

    @PostConstruct
    protected void init() {
        if (this.id == null) {
            this.id = "v1-" + StringUtils.substringAfterLast(file, "/").split("[.]")[0];
        }
    }

    protected void copyAccessors(Schema schema) {
        SchemaFactory parentSchemaFactory = schemaFactory;
        Class<?> clz =  parentSchemaFactory.getSchemaClass(schema.getId());
        if (clz == null) {
            return;
        }

        schemaClasses.put(schema.getId().toLowerCase(), clz);

        Schema parentSchema = parentSchemaFactory.getSchema(clz);
        for (Map.Entry<String, Field> entry : schema.getResourceFields().entrySet()) {
            ((FieldImpl) entry.getValue()).setName(entry.getKey());
            Field parentField = parentSchema.getResourceFields().get(entry.getKey());
            if (parentField == null || !(parentField instanceof FieldImpl)) {
                continue;
            }
            ((FieldImpl) entry.getValue()).setReadMethod(((FieldImpl) parentField).getReadMethod());
        }
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Schema> listSchemas() {
        return schemas;
    }

    @Override
    public Schema getSchema(String type) {
        if (type == null) {
            return null;
        }
        return schemaMap.get(type.toLowerCase());
    }

    @Override
    public Schema getSchema(Class<?> clz) {
        Schema s = schemaFactory.getSchema(clz);
        return s == null ? null : getSchema(s.getId());
    }

    @Override
    public Class<?> getSchemaClass(String type) {
        Schema schema = getSchema(type);
        return schema == null ? null : schemaFactory.getSchemaClass(schema.getId());
    }

    @Override
    public Schema registerSchema(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Schema parseSchema(String name) {
        throw new UnsupportedOperationException();
    }

    public void setId(String id) {
        this.id = id;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
