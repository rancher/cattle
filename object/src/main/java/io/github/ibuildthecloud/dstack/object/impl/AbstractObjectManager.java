package io.github.ibuildthecloud.dstack.object.impl;

import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.lifecycle.ObjectLifeCycleHandler;
import io.github.ibuildthecloud.dstack.object.lifecycle.ObjectLifeCycleHandler.LifeCycleEvent;
import io.github.ibuildthecloud.dstack.object.postinit.ObjectPostInstantiationHandler;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractObjectManager implements ObjectManager {

    SchemaFactory schemaFactory;
    List<ObjectPostInstantiationHandler> postInitHandlers;
    List<ObjectLifeCycleHandler> lifeCycleHandlers;

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
    public <T> T setFields(Object obj, Object key, Object... valueKeyValue) {
        Map<Object,Object> values = CollectionUtils.asMap(key, valueKeyValue);

        return setFields(obj, convertToPropertiesFor(obj, values));
    }

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

}
