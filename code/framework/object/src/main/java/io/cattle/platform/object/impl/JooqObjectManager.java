package io.cattle.platform.object.impl;

import io.cattle.platform.engine.idempotent.Idempotent;
import io.cattle.platform.engine.idempotent.IdempotentExecution;
import io.cattle.platform.engine.idempotent.IdempotentExecutionNoReturn;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.MapRelationship;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.ResultQuery;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;

public class JooqObjectManager extends AbstractObjectManager {

//    private static final Logger log = LoggerFactory.getLogger(JooqObjectManager.class);

    private static final String PLUS = "+";

    Map<ChildReferenceCacheKey, ForeignKey<?, ?>> childReferenceCache = Collections.synchronizedMap(new WeakHashMap<ChildReferenceCacheKey, ForeignKey<?, ?>>());
    Configuration configuration;
    Configuration lockingConfiguration;
    TransactionDelegate transactionDelegate;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T instantiate(Class<T> clz, Map<String,Object> properties) {
        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        UpdatableRecord<?> record = JooqUtils.getRecord(recordClass);
        record.attach(getConfiguration());

        return (T)record;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T newRecord(Class<T> type) {
        Class<?> clz = JooqUtils.getRecordClass(schemaFactory, type);
        try {
            return (T)clz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T insert(T instance, Class<T> clz, Map<String,Object> properties) {
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

    @Override
    public <T> T setFields(final Object obj, final Map<String,Object> values) {
        return Idempotent.change(new IdempotentExecution<T>() {
            @Override
            public T execute() {
                return setFieldsInternal(obj, values);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected <T> T setFieldsInternal(final Object obj, final Map<String,Object> values) {
        final List<UpdatableRecord<?>> pending = new ArrayList<UpdatableRecord<?>>();
        Map<Object,Object> toWrite = toObjectsToWrite(obj, values);
        setFields(obj, toWrite, pending);

        if ( pending.size() == 1 ) {
            persistRecord(pending.get(0));
        } else if ( pending.size() > 1 ){
            transactionDelegate.doInTransaction(new Runnable() {
                @Override
                public void run() {
                    for ( UpdatableRecord<?> record : pending ) {
                        persistRecord(record);
                    }
                }
            });
        }

        return (T)obj;
    }

    @SuppressWarnings("unchecked")
    protected void setFields(Object obj, Map<Object,Object> toWrite, List<UpdatableRecord<?>> result) {
        String type = getPossibleSubType(obj);
        Schema schema = schemaFactory.getSchema(type);

        UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);

        for ( Map.Entry<Object, Object> entry : toWrite.entrySet() ) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ( key instanceof String ) {
                String name = (String)key;
                if ( name.startsWith(PLUS) && value instanceof Map<?, ?> ) {
                    name = name.substring(PLUS.length());
                    Object mapObj = ObjectUtils.getPropertyIgnoreErrors(obj, name);
                    if ( mapObj instanceof Map<?, ?> ) {
                        mergeMap((Map<Object,Object>)value, (Map<Object,Object>)mapObj);
                    }
                    setField(schema, record, name, mapObj);
                } else {
                    setField(schema, record, (String)key, value);
                }
            } else if ( key instanceof Relationship && value instanceof Map<?,?> ) {
                Relationship rel = (Relationship)key;
                Object id = ObjectUtils.getPropertyIgnoreErrors(obj, rel.getPropertyName());
                if ( id == null ) {
                    continue;
                }
                Object refObj = loadResource(rel.getObjectType(), id);
                setFields(refObj, (Map<Object,Object>)value, result);
            }
        }

        if ( record.changed() ) {
            result.add(record);
        }
    }

    @SuppressWarnings("unchecked")
    protected void mergeMap(Map<Object,Object> src, Map<Object,Object> dest) {
        for ( Map.Entry<Object, Object> entry : src.entrySet() ) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if ( key instanceof String ) {
                String name = (String)key;
                if ( name.startsWith(PLUS) && value instanceof Map<?,?> ) {
                    name = name.substring(PLUS.length());
                    Object mapObj = dest.get(name);

                    if ( mapObj instanceof Map<?,?> ) {
                        mergeMap((Map<Object,Object>)value, (Map<Object,Object>)mapObj);
                    } else {
                        dest.put(name, value);
                    }
                } else {
                    dest.put(name, value);
                }
            }
        }
    }

    protected void persistRecord(final UpdatableRecord<?> record) {
        record.attach(getLockingConfiguration());

        if ( record.changed() ) {
            if ( record.update() == 0 ) {
                throw new IllegalStateException("Failed to update [" + record + "]");
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> List<T> children(Object obj, Class<T> type) {
        if ( obj == null ) {
            return Collections.emptyList();
        }
        UpdatableRecord<?> recordObject = JooqUtils.getRecordObject(obj);
        Class<UpdatableRecord<?>> parent = JooqUtils.getRecordClass(schemaFactory, obj.getClass());
        Class<UpdatableRecord<?>> child = JooqUtils.getRecordClass(schemaFactory, type);
        ChildReferenceCacheKey key = new ChildReferenceCacheKey(parent, child);

        ForeignKey<?, ?> foreignKey = childReferenceCache.get(key);
        if ( foreignKey == null ) {
            Table<?> childTable = JooqUtils.getTableFromRecordClass(child);
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
    public <T> List<T> mappedChildren(Object obj, Class<T> type) {
        if ( obj == null ) {
            return Collections.emptyList();
        }

        Schema schema = schemaFactory.getSchema(type);
        if ( schema != null ) {
            String typeName = schemaFactory.getSchemaName(obj.getClass());
            String linkName = schema.getPluralName();
            Relationship rel = getMetaDataManager().getRelationship(typeName, linkName);

            if ( rel != null ) {
                return getListByRelationship(obj, rel);
            }
        }
        throw new IllegalStateException("Failed to find a path from [" + obj.getClass() + "] to [" + type + "]");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> List<T> getListByRelationshipMap(Object obj, MapRelationship rel) {
        Class<UpdatableRecord<?>> typeClass = JooqUtils.getRecordClass(schemaFactory, rel.getObjectType());

        String mappingType = schemaFactory.getSchemaName(rel.getMappingType());
        String fromType = schemaFactory.getSchemaName(rel.getObjectType());

        TableField<?, Object> fieldFrom = JooqUtils.getTableField(getMetaDataManager(), fromType, ObjectMetaDataManager.ID_FIELD);
        TableField<?, Object> mappingTo = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getOtherRelationship().getPropertyName());
        TableField<?, Object> mappingOther = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getPropertyName());
        TableField<?, Object> mappingRemoved = JooqUtils.getTableField(getMetaDataManager(), mappingType, ObjectMetaDataManager.REMOVED_FIELD);

        Table<?> table = JooqUtils.getTable(schemaFactory, typeClass);
        Table<?> mapTable = JooqUtils.getTable(schemaFactory, rel.getMappingType());

        SelectQuery<?> query = create().selectQuery();
        query.addFrom(table);
        query.addSelect(table.fields());
        query.addJoin(mapTable, fieldFrom.eq(mappingTo)
                .and(mappingRemoved == null ? DSL.trueCondition() : mappingRemoved.isNull())
                .and(mappingOther.eq(ObjectUtils.getId(obj))));

        return (List<T>)query.fetchInto(typeClass);
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
            String name = metaDataManager.convertToPropertyNameString(recordClass, entry.getKey());
            if ( name != null ) {
                result.put(name, entry.getValue());
            }
        }

        return result;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T findOne(Class<T> clz, Map<Object, Object> values) {
        return (T)toQuery(clz, values).fetchOne();
    }

    @Override
    public <T> T findOne(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object,Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return findOne(clz, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findAny(Class<T> clz, Map<Object, Object> values) {
        return (T)toQuery(clz, values).fetchAny();
    }

    @Override
    public <T> T findAny(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object,Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return findAny(clz, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> find(Class<T> clz, Map<Object, Object> values) {
        return (List<T>)toQuery(clz, values).fetch();
    }

    @Override
    public <T> List<T> find(Class<T> clz, Object key, Object... valueKeyValue) {
        Map<Object,Object> map = CollectionUtils.asMap(key, valueKeyValue);
        return find(clz, map);
    }

    protected ResultQuery<?> toQuery(Class<?> clz, Map<Object, Object> values) {
        String type = schemaFactory.getSchemaName(clz);
        if ( type == null ) {
            throw new IllegalArgumentException("Failed to find type of class [" + clz + "]");
        }
        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        Table<?> table = JooqUtils.getTableFromRecordClass(recordClass);
        return create()
                .selectFrom(table)
                .where(JooqUtils.toConditions(metaDataManager, type, values));
    }

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    protected void setField(Schema schema, Object obj, String name, Object value) {
        try {
            if ( PropertyUtils.getPropertyDescriptor(obj, name) == null ) {
                if ( schema != null && schema.getResourceFields().containsKey(name) ) {
                    DataAccessor
                        .fields(obj)
                        .withKey(name)
                        .set(value);
                }
            } else {
                /* BeanUtils doesn't always like setting null values */
                if ( value == null ) {
                    PropertyUtils.setProperty(obj, name, value);
                } else {
                    BeanUtils.setProperty(obj, name, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to set [" + name + "] to value [" + value + "] on [" + obj + "]");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set [" + name + "] to value [" + value + "] on [" + obj + "]");
        } catch (NoSuchMethodException e) {
            // Ignore if it doesn't exists
        }
    }

    @Override
    public void delete(Object obj) {
        if ( obj == null ) {
            return;
        }

        Object id = ObjectUtils.getId(obj);
        String type = getType(obj);
        Table<?> table = JooqUtils.getTableFromRecordClass(JooqUtils.getRecordClass(getSchemaFactory(), obj.getClass()));
        TableField<?, Object> idField = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ID_FIELD);

        int result = create()
                .delete(table)
                .where(idField.eq(id))
                .execute();

        if ( result != 1 ) {
            throw new IllegalStateException("Failed to delete [" + type + "] id [" + id + "]");
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
        return (T) loadResource(clz, resourceId);
    }

    @Override
    public <T> T loadResource(Class<T> type, Long resourceId) {
        return loadResource(type, (Object)resourceId);
    }

    @Override
    public <T> T loadResource(Class<T> type, String resourceId) {
        return loadResource(type, (Object)resourceId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadResource(Class<T> type, Object resourceId) {
        if ( resourceId == null || type == null ) {
            return null;
        }

        Class<UpdatableRecord<?>> clz = JooqUtils.getRecordClass(schemaFactory, type);
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


    public TransactionDelegate getTransactionDelegate() {
        return transactionDelegate;
    }

    @Inject
    public void setTransactionDelegate(TransactionDelegate transactionDelegate) {
        this.transactionDelegate = transactionDelegate;
    }

}
