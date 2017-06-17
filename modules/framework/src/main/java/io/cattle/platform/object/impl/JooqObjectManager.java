package io.cattle.platform.object.impl;

import io.cattle.platform.engine.idempotent.Idempotent;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.ResultQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class JooqObjectManager extends AbstractObjectManager {

    Map<ChildReferenceCacheKey, ForeignKey<?, ?>> childReferenceCache = Collections
            .synchronizedMap(new WeakHashMap<ChildReferenceCacheKey, ForeignKey<?, ?>>());
    Configuration configuration;
    Configuration lockingConfiguration;

    public JooqObjectManager(SchemaFactory schemaFactory, ObjectMetaDataManager metaDataManager, Configuration configuration,
            Configuration lockingConfiguration) {
        super(schemaFactory, metaDataManager);
        this.configuration = configuration;
        this.lockingConfiguration = lockingConfiguration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T instantiate(Class<T> clz, Map<String, Object> properties) {
        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        UpdatableRecord<?> record = JooqUtils.getRecord(recordClass);
        record.attach(getConfiguration());

        return (T) record;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newRecord(Class<T> type) {
        Class<?> clz = JooqUtils.getRecordClass(schemaFactory, type);
        try {
            return (T) clz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T insert(T instance, Class<T> clz, Map<String, Object> properties) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(instance);
        record.attach(configuration);
        Idempotent.change(record::insert);
        return instance;
    }

    @Override
    public <T> T reload(T obj) {
        if (obj == null) {
            return null;
        }
        UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        record.refresh();
        return obj;
    }

    @Override
    public <T> T persist(T obj) {
        final UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        record.update();
        Idempotent.change(() -> persistRecord(record));
        return obj;
    }

    @Override
    public <T> T setFields(final T obj, final Map<String, Object> values) {
        return setFields(null, obj, values);
    }

    @Override
    public <T> T setFields(Schema schema, T obj, Map<String, Object> values) {
        return Idempotent.change(() -> setFieldsInternal(schema, obj, values));
    }

    protected <T> T setFieldsInternal(final Schema schema, final T obj, final Map<String, Object> values) {
        persistFields(schema, obj, values);
        return obj;
    }

    protected void persistFields(Schema schema, Object obj, Map<String, Object> values) {
        if (schema == null) {
            String type = getPossibleSubType(obj, values);
            schema = schemaFactory.getSchema(type);
        }

        UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            setField(schema, record, entry.getKey(), entry.getValue());
        }

        persistRecord(record);
    }

    protected UpdatableRecord<?> persistRecord(final UpdatableRecord<?> record) {
        if (record.field(ObjectMetaDataManager.DATA_FIELD) != null && record.changed(ObjectMetaDataManager.DATA_FIELD)) {
            record.attach(getLockingConfiguration());
        } else if (record.field(ObjectMetaDataManager.STATE_FIELD) != null && record.changed(ObjectMetaDataManager.STATE_FIELD)) {
            record.attach(getLockingConfiguration());
        } else {
            record.attach(getConfiguration());
        }

        if (record.changed()) {
            if (record.update() == 0) {
                throw new IllegalStateException("Failed to update [" + record + "]");
            }
        }

        return record;
    }

    @Override
    public <T> List<T> children(Object obj, Class<T> type) {
        return children(obj, type, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> List<T> children(Object obj, Class<T> type, String propertyName) {
        if (obj == null) {
            return Collections.emptyList();
        }
        UpdatableRecord<?> recordObject = JooqUtils.getRecordObject(obj);
        Class<UpdatableRecord<?>> parent = JooqUtils.getRecordClass(schemaFactory, obj.getClass());
        Class<UpdatableRecord<?>> child = JooqUtils.getRecordClass(schemaFactory, type);
        ChildReferenceCacheKey key = new ChildReferenceCacheKey(parent, child, propertyName);
        TableField<?, ?> propertyField = (TableField<?, ?>) (propertyName == null ? null : metaDataManager.convertFieldNameFor(getType(type), propertyName));

        ForeignKey<?, ?> foreignKey = childReferenceCache.get(key);
        if (foreignKey == null) {
            Table<?> childTable = JooqUtils.getTableFromRecordClass(child);
            for (ForeignKey<?, ?> foreignKeyTest : childTable.getReferences()) {
                if (foreignKeyTest.getKey().getTable().getRecordType() == parent) {
                    if (propertyField != null) {
                        if (foreignKeyTest.getFields().get(0).getName().equals(propertyField.getName())) {
                            if (foreignKey != null) {
                                throw new IllegalStateException("Found more that one foreign key from [" + child + "] to [" + parent + "]");
                            }
                            foreignKey = foreignKeyTest;
                        }
                    } else if (foreignKey == null) {
                        foreignKey = foreignKeyTest;
                    } else {
                        throw new IllegalStateException("Found more that one foreign key from [" + child + "] to [" + parent + "]");
                    }
                }
            }

            if (foreignKey == null) {
                throw new IllegalStateException("Failed to find a foreign key from [" + child + "] to [" + parent + "]");
            }

            childReferenceCache.put(key, foreignKey);
        }

        recordObject.attach(getConfiguration());
        return recordObject.fetchChildren((ForeignKey) foreignKey);
    }

    @Override
    public Map<String, Object> convertToPropertiesFor(Object obj, Map<Object, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> recordClass;

        if (obj instanceof Class<?>) {
            recordClass = JooqUtils.getRecordClass(schemaFactory, (Class<?>) obj);
        } else {
            UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
            recordClass = JooqUtils.getRecordClass(schemaFactory, record.getClass());
        }

        for (Map.Entry<Object, Object> entry : values.entrySet()) {
            String name = metaDataManager.convertToPropertyNameString(recordClass, entry.getKey());
            if (name != null) {
                result.put(name, entry.getValue());
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findOne(Class<T> clz, Map<Object, Object> values) {
        return (T) toQuery(clz, values).fetchOne();
    }

    @Override
    public <T> T findOne(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object, Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return findOne(clz, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findAny(Class<T> clz, Map<Object, Object> values) {
        return (T) toQuery(clz, values).fetchAny();
    }

    @Override
    public <T> T findAny(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object, Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return findAny(clz, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> find(Class<T> clz, Map<Object, Object> values) {
        return (List<T>) toQuery(clz, values).fetch();
    }

    @Override
    public <T> List<T> find(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object, Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return find(clz, map);
    }

    protected ResultQuery<?> toQuery(Class<?> clz, Map<Object, Object> values) {
        String type = schemaFactory.getSchemaName(clz);
        if (type == null) {
            throw new IllegalArgumentException("Failed to find type of class [" + clz + "]");
        }
        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        Table<?> table = JooqUtils.getTableFromRecordClass(recordClass);
        return create().selectFrom(table).where(JooqUtils.toConditions(metaDataManager, type, values));
    }

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    protected void setField(Schema schema, Object obj, String name, Object value) {
        try {
            if (PropertyUtils.getPropertyDescriptor(obj, name) == null) {
                if (schema != null && schema.getResourceFields().containsKey(name)) {
                    DataAccessor.fields(obj).withKey(name).set(value);
                }
            } else {
                /* BeanUtils doesn't always like setting null values */
                if (value == null) {
                    PropertyUtils.setProperty(obj, name, null);
                } else {
                    BeanUtils.setProperty(obj, name, value);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set [" + name + "] to value [" + value + "] on [" + obj + "]");
        } catch (NoSuchMethodException e) {
            // Ignore if it doesn't exists
        }
    }

    @Override
    public void delete(Object obj) {
        if (obj == null) {
            return;
        }

        Object id = ObjectUtils.getId(obj);
        String type = getType(obj);
        Table<?> table = JooqUtils.getTableFromRecordClass(JooqUtils.getRecordClass(getSchemaFactory(), obj.getClass()));
        TableField<?, Object> idField = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ID_FIELD);

        if (idField == null) {
            throw new IllegalStateException("No ID field to delete object [" + obj + "]");
        }

        create().delete(table).where(idField.eq(id)).execute();
    }

    @Override
    public <T> T loadResource(String resourceType, String resourceId) {
        return loadResource(resourceType, (Object) resourceId);
    }

    @Override
    public <T> T loadResource(String resourceType, Long resourceId) {
        return loadResource(resourceType, (Object) resourceId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadResource(String resourceType, Object resourceId) {
        Class<?> clz = schemaFactory.getSchemaClass(resourceType);
        return (T) loadResource(clz, resourceId);
    }

    @Override
    public <T> T loadResource(Class<T> type, Long resourceId) {
        return loadResource(type, (Object) resourceId);
    }

    @Override
    public <T> T loadResource(Class<T> type, String resourceId) {
        return loadResource(type, (Object) resourceId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadResource(Class<T> type, Object resourceId) {
        if (resourceId == null || type == null) {
            return null;
        }

        Class<UpdatableRecord<?>> clz = JooqUtils.getRecordClass(schemaFactory, type);
        return (T) JooqUtils.findById(DSL.using(getConfiguration()), clz, resourceId);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Configuration getLockingConfiguration() {
        return lockingConfiguration;
    }

}
