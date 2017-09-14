package io.cattle.platform.object.impl;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.postinit.ObjectPostInstantiationHandler;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractObjectManager implements ObjectManager {

    SchemaFactory schemaFactory;
    List<ObjectPostInstantiationHandler> postInitHandlers = new ArrayList<>();
    ObjectMetaDataManager metaDataManager;

    public AbstractObjectManager(SchemaFactory schemaFactory, ObjectMetaDataManager metaDataManager) {
        super();
        this.schemaFactory = schemaFactory;
        this.metaDataManager = metaDataManager;
    }

    @Override
    public <T> T create(T instance) {
        return create(instance, new HashMap<>());
    }

    @Override
    public <T> T create(T instance, Object key, Object... valueKeyValue) {
        Map<Object, Object> properties = CollectionUtils.asMap(key, valueKeyValue);
        return create(instance, convertToPropertiesFor(instance, properties));
    }

    @Override
    public <T> T create(T instance, Map<String, Object> properties) {
        @SuppressWarnings("unchecked")
        Class<T> clz = (Class<T>) instance.getClass();

        for (ObjectPostInstantiationHandler handler : postInitHandlers) {
            instance = handler.postProcess(instance, clz, properties);
        }

        return insert(instance, clz, properties);
    }

    @Override
    public <T> T create(Class<T> clz, Map<String, Object> properties) {
        T instance = construct(clz, properties);

        return insert(instance, clz, properties);
    }

    @Override
    public <T> T create(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object, Object> properties = CollectionUtils.asMap(key, valueKeyValue);
        return create(clz, convertToPropertiesFor(clz, properties));
    }

    protected <T> T construct(Class<T> clz, Map<String, Object> properties) {
        T instance = instantiate(clz, properties);

        for (ObjectPostInstantiationHandler handler : postInitHandlers) {
            instance = handler.postProcess(instance, clz, properties);
        }

        return instance;
    }

    protected abstract <T> T instantiate(Class<T> clz, Map<String, Object> properties);

    protected abstract <T> T insert(T instance, Class<T> clz, Map<String, Object> properties);

    @Override
    public String getType(Object obj) {
        if (obj == null) {
            return null;
        }

        Schema schema;
        if (obj instanceof Class<?>) {
            schema = schemaFactory.getSchema((Class<?>) obj);
        } else if (obj instanceof String) {
            schema = schemaFactory.getSchema(obj.toString());
        } else {
            schema = schemaFactory.getSchema(obj.getClass());
        }
        if (schema == null) {
            return null;
        }

        return schema.getParent() == null ? schema.getId() : schemaFactory.getBaseType(schema.getId());
    }

    protected String getPossibleSubType(Object obj, Map<String, Object> values) {
        Object kind = values.get(ObjectMetaDataManager.KIND_FIELD);
        if (kind == null) {
            kind = ObjectUtils.getPropertyIgnoreErrors(obj, ObjectMetaDataManager.KIND_FIELD);
        }

        if (kind != null) {
            String kindString = kind.toString();
            String baseType = schemaFactory.getBaseType(kindString);
            Class<?> clz = schemaFactory.getSchemaClass(baseType);

            if (clz != null && clz.isAssignableFrom(obj.getClass())) {
                return kindString;
            }
        }

        return getType(obj);
    }

    @Override
    public <T> T setFields(T obj, Object key, Object... valueKeyValue) {
        if (obj == null) {
            return null;
        }

        Map<Object, Object> values = CollectionUtils.asMap(key, valueKeyValue);
        return setFields(obj, convertToPropertiesFor(obj, values));
    }

    @Override
    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public List<ObjectPostInstantiationHandler> getPostInitHandlers() {
        return postInitHandlers;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

}
