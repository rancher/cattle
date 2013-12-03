package io.github.ibuildthecloud.dstack.object.impl;

import io.github.ibuildthecloud.dstack.engine.idempotent.Idempotent;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentExecutionNoReturn;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.jooq.Configuration;
import org.jooq.ForeignKey;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.exception.DataChangedException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqObjectManager extends AbstractObjectManager {

    private static final Logger log = LoggerFactory.getLogger(JooqObjectManager.class);

    Map<ChildReferenceCacheKey, ForeignKey<?, ?>> childReferenceCache = new WeakHashMap<ChildReferenceCacheKey, ForeignKey<?, ?>>();
    Configuration configuration;
    Configuration lockingConfiguration;
    ObjectMetaDataManager metaDataManager;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T instantiate(Class<T> clz, Object properties) {
        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        UpdatableRecord<?> record = JooqUtils.getRecord(recordClass);
        record.attach(getConfiguration());

        return (T)record;
    }

    @Override
    public <T> T insert(T instance, Class<T> clz, Object properties) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(instance);
        record.attach(configuration);
        Idempotent.change(new IdempotentExecutionNoReturn() {
            @Override
            protected void executeNoResult() {
                record.insert();
            }
        });
        return instance;
    }

    @Override
    public <T> T reload(T obj) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        Idempotent.change(new IdempotentExecutionNoReturn() {
            @Override
            protected void executeNoResult() {
                record.refresh();
            }
        });
        return obj;
    }

    @Override
    public <T> T persist(T obj) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        record.update();
        Idempotent.change(new IdempotentExecutionNoReturn() {
            @Override
            protected void executeNoResult() {
                if ( record.changed() ) {
                    record.update();
                }
            }
        });
        return obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T setFields(final Object obj, final Map<String,Object> values) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);

        for ( Map.Entry<String, Object> entry : values.entrySet() ) {
            setField(obj, entry.getKey(), entry.getValue());
        }

        record.attach(getLockingConfiguration());
        try {
            Idempotent.change(new IdempotentExecutionNoReturn() {
                @Override
                protected void executeNoResult() {
                    if ( record.changed() ) {
                        if ( record.update() == 0 ) {
                            throw new IllegalStateException("Failed to update [" + obj + "] with " + values);
                        }
                    }
                }
            });
        } catch ( DataChangedException e ) {
            log.info("Data changed and can not set fields [{}] on [{}]", values, obj);
            return null;
        }

        return (T)record;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> List<T> children(Object obj, Class<T> type) {
        UpdatableRecord<?> recordObject = JooqUtils.getRecordObject(obj);
        Class<UpdatableRecord<?>> parent = JooqUtils.getRecordClass(schemaFactory, obj.getClass());
        Class<UpdatableRecord<?>> child = JooqUtils.getRecordClass(schemaFactory, type);
        ChildReferenceCacheKey key = new ChildReferenceCacheKey(parent, child);

        ForeignKey<?, ?> foreignKey = childReferenceCache.get(key);
        if ( foreignKey == null ) {
            Table<?> childTable = JooqUtils.getTable(child);
            for ( ForeignKey<?, ?> foreignKeyTest : childTable.getReferences() ) {
                if ( foreignKeyTest.getKey().getTable().getRecordType() == parent ) {
                    if ( foreignKey == null ) {
                        foreignKey = foreignKeyTest;
                    } else {
                        throw new IllegalStateException("Found more that one foreign key from [" + child + "] to [" + parent + "]");
                    }
                }
            }

            if ( foreignKey == null ) {
                throw new IllegalStateException("Failed to find a foreign key from [" + child + "] to [" + parent + "]");
            }

            childReferenceCache.put(key, foreignKey);
        }

        recordObject.attach(getConfiguration());
        return recordObject.fetchChildren((ForeignKey)foreignKey);
    }


    @Override
    public Map<String,Object> convertToPropertiesFor(Object obj, Map<Object,Object> values) {
        Map<String,Object> result = new LinkedHashMap<String, Object>();
        Class<?> recordClass = null;

        if ( obj instanceof Class<?> ) {
            recordClass = JooqUtils.getRecordClass(schemaFactory, (Class<?>)obj);
        } else {
            UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
            recordClass = JooqUtils.getRecordClass(schemaFactory, record.getClass());
        }

        for ( Map.Entry<Object, Object> entry : values.entrySet() ) {
            String name = metaDataManager.convertPropertyNameFor(recordClass, entry.getKey());
            result.put(name, entry.getValue());
        }

        return result;
    }

    protected void setField(Object obj, String name, Object value) {
        try {
            BeanUtils.setProperty(obj, name, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to set [" + name + "] to value [" + value + "] on [" + obj + "]");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set [" + name + "] to value [" + value + "] on [" + obj + "]");
        }
    }


    @Override
    public <T> T loadResource(String resourceType, String resourceId) {
        return loadResource(resourceType, (Object)resourceId);
    }

    @Override
    public <T> T loadResource(String resourceType, Long resourceId) {
        return loadResource(resourceType, (Object)resourceId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadResource(String resourceType, Object resourceId) {
        Class<?> clz = schemaFactory.getSchemaClass(resourceType);
        return (T) JooqUtils.findById(DSL.using(getConfiguration()), clz, resourceId);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getLockingConfiguration() {
        return lockingConfiguration;
    }

    @Inject
    public void setLockingConfiguration(Configuration lockingConfiguration) {
        this.lockingConfiguration = lockingConfiguration;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

}
