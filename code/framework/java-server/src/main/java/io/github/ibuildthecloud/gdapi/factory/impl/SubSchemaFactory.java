package io.github.ibuildthecloud.gdapi.factory.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Filter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicBooleanProperty;

public class SubSchemaFactory extends AbstractSchemaFactory implements SchemaFactory {

    private static DynamicBooleanProperty SERIALIZE = ArchaiusUtil.getBoolean("serialize.schemas");

    SchemaFactory schemaFactory;
    String id;
    Map<String, Schema> schemaMap;
    List<SchemaImpl> schemaList = new ArrayList<SchemaImpl>();
    List<SchemaPostProcessor> postProcessors = new ArrayList<SchemaPostProcessor>();
    boolean init = false;

    public synchronized void init() {
        if (init) {
            return;
        }

        schemaMap = new HashMap<String, Schema>();

        if (schemaFactory instanceof SubSchemaFactory) {
            ((SubSchemaFactory)schemaFactory).init();
        }

        List<SchemaImpl> result = new ArrayList<SchemaImpl>();

        for (Schema schema : schemaFactory.listSchemas()) {
            if (schema instanceof SchemaImpl) {
                /* Copy */
                SchemaImpl impl = new SchemaImpl((SchemaImpl)schema);

                for (SchemaPostProcessor post : postProcessors) {
                    impl = post.postProcessRegister(impl, this);
                    if (impl == null) {
                        break;
                    }
                }

                if (impl != null) {
                    result.add(impl);
                    schemaMap.put(impl.getId(), impl);
                }
            }
        }

        schemaList = result;

        for (SchemaImpl schema : schemaList) {
            for (SchemaPostProcessor post : postProcessors) {
                schema = post.postProcess(schema, this);
            }
        }

        for (SchemaImpl schema : schemaList) {
            prune(schema);
        }

        if (SERIALIZE.get()) {
            serializeSchema(listSchemas(), getId());
            synchronized (schemaFactory) {
                serializeSchema(schemaFactory.listSchemas(), schemaFactory.getId());
            }
        }

        init = true;
    }

    protected static void serializeSchema(List<Schema> schemaList, String id) {
        try(FileOutputStream fos = new FileOutputStream(new File(id + ".ser"))) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(schemaList);
            oos.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void prune(SchemaImpl schema) {
        Map<String, Field> fields = schema.getResourceFields();
        Map<String, Filter> filters = schema.getCollectionFilters();

        for (String name : new HashSet<String>(fields.keySet())) {
            Field field = fields.get(name);

            List<FieldType> subTypeEnums = field.getSubTypeEnums();
            List<String> subTypes = field.getSubTypes();

            for (int i = 0; i < subTypeEnums.size(); i++) {
                if (subTypeEnums.get(i) == FieldType.TYPE && !schemaMap.containsKey(subTypes.get(i)) && !"type".equals(subTypes.get(i))) {
                    fields.remove(name);
                    filters.remove(name);
                    break;
                }
            }
        }

        Iterator<String> childrenIter = schema.getChildren().iterator();
        while (childrenIter.hasNext()) {
            if (!schemaMap.containsKey(childrenIter.next())) {
                childrenIter.remove();
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<Schema> listSchemas() {
        return (List)schemaList;
    }

    @Override
    public Schema getSchema(Class<?> clz) {
        return getSchema(schemaFactory.getSchemaName(clz));
    }

    @Override
    public Schema getSchema(String type) {
        Schema parentSchema = schemaFactory.getSchema(type);
        return parentSchema == null ? null : schemaMap.get(parentSchema.getId());
    }

    @Override
    public Class<?> getSchemaClass(String type) {
        Schema schema = getSchema(type);
        return schema == null ? null : schemaFactory.getSchemaClass(type);
    }

    @Override
    public Schema registerSchema(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Schema parseSchema(String name) {
        throw new UnsupportedOperationException();
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public List<SchemaPostProcessor> getPostProcessors() {
        return postProcessors;
    }

    public void setPostProcessors(List<SchemaPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
