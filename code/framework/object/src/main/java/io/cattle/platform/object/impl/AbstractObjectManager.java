package io.cattle.platform.object.impl;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.lifecycle.ObjectLifeCycleHandler;
import io.cattle.platform.object.lifecycle.ObjectLifeCycleHandler.LifeCycleEvent;
import io.cattle.platform.object.meta.MapRelationship;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.meta.Relationship.RelationshipType;
import io.cattle.platform.object.postinit.ObjectPostInstantiationHandler;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractObjectManager implements ObjectManager {

    SchemaFactory schemaFactory;
    List<ObjectPostInstantiationHandler> postInitHandlers;
    List<ObjectLifeCycleHandler> lifeCycleHandlers;
    ObjectMetaDataManager metaDataManager;


    @Override
    public <T> T create(T instance) {
        return create(instance, new HashMap<String, Object>());
    }

    @Override
    public <T> T create(T instance, Object key, Object... valueKeyValue) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, valueKeyValue);
        return create(instance, convertToPropertiesFor(instance, properties));
    }

    @Override
    public <T> T create(T instance, Map<String,Object> properties) {
        @SuppressWarnings("unchecked")
        Class<T> clz = (Class<T>) instance.getClass();

        for ( ObjectPostInstantiationHandler handler : postInitHandlers ) {
            instance = handler.postProcess(instance, clz, properties);
        }

        instance = insert(instance, clz, properties);

        instance = callLifeCycleHandlers(LifeCycleEvent.CREATE, instance, clz, properties);

        return instance;
    }

    @Override
    public <T> T create(Class<T> clz, Map<String,Object> properties) {
        T instance = construct(clz, properties);

        instance = insert(instance, clz, properties);

        instance = callLifeCycleHandlers(LifeCycleEvent.CREATE, instance, clz, properties);

        return instance;
    }


    @Override
    public <T> T create(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, valueKeyValue);
        return create(clz, convertToPropertiesFor(clz, properties));
    }

    protected <T> T construct(Class<T> clz, Map<String,Object> properties) {
        T instance = instantiate(clz, properties);

        for ( ObjectPostInstantiationHandler handler : postInitHandlers ) {
            instance = handler.postProcess(instance, clz, properties);
        }

        return instance;
    }

    protected <T> T callLifeCycleHandlers(LifeCycleEvent event, T instance, Class<T> clz, Map<String,Object> properties) {
        for ( ObjectLifeCycleHandler handler : lifeCycleHandlers ) {
            instance = handler.onEvent(event, instance, clz, properties);
        }
        return instance;
    }

    protected abstract <T> T instantiate(Class<T> clz, Map<String,Object> properties);

    protected abstract <T> T insert(T instance, Class<T> clz, Map<String,Object> properties);

    @Override
    public String getType(Object obj) {
        if ( obj == null ) {
            return null;
        }

        Schema schema = null;
        if ( obj instanceof Class<?> ) {
            schema = schemaFactory.getSchema((Class<?>)obj);
        } else {
            schema = schemaFactory.getSchema(obj.getClass());
        }
        if ( schema == null ) {
            return null;
        }

        return schema.getParent() == null ? schema.getId () : schemaFactory.getBaseType(schema.getId());
    }

    protected String getPossibleSubType(Object obj) {
        Object kind = ObjectUtils.getPropertyIgnoreErrors(obj, ObjectMetaDataManager.KIND_FIELD);

        if ( kind != null ) {
            String kindString = kind.toString();
            String baseType = schemaFactory.getBaseType(kindString);
            Class<?> clz = schemaFactory.getSchemaClass(baseType);

            if ( kind != null && clz != null && clz.isAssignableFrom(obj.getClass()) ) {
                return kindString;
            }
        }

        return getType(obj);
    }

    @Override
    public <T> T setFields(Object obj, Object key, Object... valueKeyValue) {
        Map<Object,Object> values = CollectionUtils.asMap(key, valueKeyValue);

        return setFields(obj, convertToPropertiesFor(obj, values));
    }

    @SuppressWarnings("unchecked")
    protected Map<Object,Object> toObjectsToWrite(Object obj, Map<String, Object> values) {
        String type = getType(obj);
        Map<String,Relationship> relationships = null;
        Map<Object,Object> objValues = new LinkedHashMap<Object, Object>();

        for ( Map.Entry<String, Object> entry : values.entrySet() ) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ( value instanceof Map<?, ?> ) {
                if ( relationships == null ) {
                    relationships = metaDataManager.getLinkRelationships(schemaFactory, type);
                }
                Relationship rel = relationships.get(key.toLowerCase());
                if ( rel != null && rel.getRelationshipType() != Relationship.RelationshipType.REFERENCE ) {
                    rel = null;
                }

                if ( rel == null ) {
                    objValues.put(key, value);
                } else {
                    value = toObjectsToWrite(rel.getObjectType(), (Map<String,Object>)value);
                    objValues.put(rel, value);
                }
            } else {
                objValues.put(key, value);
            }
        }

        return objValues;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getListByRelationship(Object obj, Relationship rel) {
        if ( rel == null || obj == null ) {
            return Collections.emptyList();
        }

        if ( ! rel.isListResult() ) {
            throw new IllegalArgumentException("Relationation arguement is not a list result");
        }

        if (  rel.getRelationshipType() == RelationshipType.CHILD ) {
            return (List<T>)children(obj, rel.getObjectType());
        } else if ( rel.getRelationshipType() == RelationshipType.MAP ) {
            return getListByRelationshipMap(obj, (MapRelationship)rel);
        }

        return Collections.emptyList();
    }

    protected abstract <T> List<T> getListByRelationshipMap(Object obj, MapRelationship rel);

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObjectByRelationship(Object obj, Relationship rel) {
        if ( rel == null || obj == null ) {
            return null;
        }

        if ( rel.isListResult() ) {
            throw new IllegalArgumentException("Relationation arguement is not a singular result");
        }

        Object id = ObjectUtils.getProperty(obj, rel.getPropertyName());

        return id == null ? null : (T)loadResource(rel.getObjectType(), id.toString());
    }

    @Override
    public boolean isKind(Object obj, String kind) {
        if ( obj == null ) {
            return false;
        }

        Object kindField = ObjectUtils.getPropertyIgnoreErrors(obj, ObjectMetaDataManager.KIND_FIELD);
        /* At some point should check hierarchies... */
        return kindField != null && kindField.equals(kind);
    }

    @Override
    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }


    public List<ObjectPostInstantiationHandler> getPostInitHandlers() {
        return postInitHandlers;
    }

    @Inject
    public void setPostInitHandlers(List<ObjectPostInstantiationHandler> postInitHandlers) {
        this.postInitHandlers = postInitHandlers;
    }

    public List<ObjectLifeCycleHandler> getLifeCycleHandlers() {
        return lifeCycleHandlers;
    }

    @Inject
    public void setLifeCycleHandlers(List<ObjectLifeCycleHandler> lifeCycleHandlers) {
        this.lifeCycleHandlers = lifeCycleHandlers;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

}
