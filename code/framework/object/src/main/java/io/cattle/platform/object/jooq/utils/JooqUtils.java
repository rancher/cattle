package io.cattle.platform.object.jooq.utils;

import java.util.List;
import java.util.Map;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.UniqueKey;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqUtils {

    private static final Logger log = LoggerFactory.getLogger(JooqUtils.class);

    @SuppressWarnings("unchecked")
    public static <T extends UpdatableRecord<?>> T findById(DSLContext context, Class<T> clz, Object id) {
        if ( id == null )
            return null;

        Table<?> table = getTableFromRecordClass(clz);
        if ( table == null )
            return null;

        UniqueKey<?> key = table.getPrimaryKey();
        if ( key == null || key.getFieldsArray().length != 1 )
            return null;

        TableField<?, Object> keyField = (TableField<?, Object>)key.getFieldsArray()[0];

        /* Convert object because we are abusing type safety here */
        Object converted = keyField.getDataType().convert(id);

        return (T)context.selectFrom(table)
                .where(keyField.eq(converted))
                .fetchOne();
    }


    public static Table<?> getTable(SchemaFactory schemaFactory, Class<?> clz) {
        return getTableFromRecordClass(getRecordClass(schemaFactory, clz));
    }

    @SuppressWarnings("unchecked")
    public static Table<?> getTableFromRecordClass(Class<?> clz) {
        if ( clz == null )
            return null;

        if ( TableRecord.class.isAssignableFrom(clz) ) {
            try {
                TableRecord<?> record =
                        ((Class<TableRecord<?>>)clz).newInstance();
                return record.getTable();
            } catch (InstantiationException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            } catch (IllegalAccessException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Class<UpdatableRecord<?>> getRecordClass(SchemaFactory factory, Class<?> clz) {
        if ( UpdatableRecord.class.isAssignableFrom(clz) ) {
            return (Class<UpdatableRecord<?>>)clz;
        }

        if ( factory != null ) {
            Schema schema = factory.getSchema(clz);
            Class<?> testClz = factory.getSchemaClass(schema.getId());
            if ( clz.isAssignableFrom(testClz) ) {
                if ( ! UpdatableRecord.class.isAssignableFrom(testClz) ) {
                    throw new IllegalArgumentException("Class [" + testClz + "] is not an instanceof UpdatableRecord");
                }
                return (Class<UpdatableRecord<?>>) testClz;
            }
        }

        throw new IllegalArgumentException("Failed to find UpdatableRecord class for [" + clz + "]");
    }

    public static UpdatableRecord<?> getRecord(Class<UpdatableRecord<?>> clz) {
        try {
            return clz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to instantiate [" + clz + "]", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to instantiate [" + clz + "]", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends UpdatableRecord<?>> T getRecordObject(Object obj) {
        if ( obj == null )
            return null;

        if ( obj instanceof UpdatableRecord<?> ) {
            return (T)obj;
        }
        throw new IllegalArgumentException("Expected instance of [" + UpdatableRecord.class + "] got [" + obj.getClass() + "]");
    }

    public static org.jooq.Condition toConditions(ObjectMetaDataManager metaData, String type, Map<Object, Object> criteria) {
        org.jooq.Condition existingCondition = null;

        for ( Map.Entry<Object, Object> entry : criteria.entrySet() ) {
            Object value = entry.getValue();
            Object key = entry.getKey();
            TableField<?, Object> field = null;
            if ( key == org.jooq.Condition.class ) {
                if ( ! ( value instanceof org.jooq.Condition ) ) {
                    throw new IllegalArgumentException("If key is Condition, value must be an instanceof Condition got key [" +
                            key + "] value [" + value + "]");
                }
            } else {
                field = getTableField(metaData, type, key);
                if ( field == null ) {
                    continue;
                }
            }

            org.jooq.Condition newCondition = null;

            if ( value instanceof org.jooq.Condition ) {
                newCondition = (org.jooq.Condition)value;
            } else if ( value instanceof Condition ) {
                newCondition = toCondition(field, (Condition)value);
            } else if ( value instanceof List ) {
                newCondition = listToCondition(field, (List<?>)value);
            } else if ( value == null ) {
                newCondition = field.isNull();
            } else {
                newCondition = field.eq(value);
            }

            if ( existingCondition == null ) {
                existingCondition = newCondition;
            } else {
                existingCondition = existingCondition.and(newCondition);
            }
        }

        return existingCondition;
    }

    @SuppressWarnings("unchecked")
    public static TableField<?, Object> getTableField(ObjectMetaDataManager metaData, String type, Object key) {
        Object objField = metaData.convertFieldNameFor(type, key);
        if ( objField instanceof TableField ) {
            return (TableField<?, Object>)objField;
        } else {
            return null;
        }
    }

    protected static org.jooq.Condition listToCondition(TableField<?, Object> field, List<?> list) {
        org.jooq.Condition condition = null;
        for ( Object value : list ) {
            if ( value instanceof Condition ) {
                org.jooq.Condition newCondition = toCondition(field, (Condition)value);
                condition = condition == null ? newCondition : condition.and(newCondition);
            } else {
                condition = condition == null ? field.eq(value) : condition.and(field.eq(value));
            }
        }

        return condition;
    }

    protected static org.jooq.Condition toCondition(TableField<?, Object> field, Condition value) {
        Condition condition = value;
        switch (condition.getConditionType()) {
        case EQ:
            return field.eq(condition.getValue());
        case GT:
            return field.gt(condition.getValue());
        case GTE:
            return field.ge(condition.getValue());
        case IN:
            List<Object> values = condition.getValues();
            if ( values.size() == 1 ) {
                return field.eq(values.get(0));
            } else {
                return field.in(condition.getValues());
            }
        case LIKE:
            return field.like(condition.getValue().toString());
        case LT:
            return field.lt(condition.getValue());
        case LTE:
            return field.le(condition.getValue());
        case NE:
            return field.ne(condition.getValue());
        case NOTLIKE:
            return field.notLike(condition.getValue().toString());
        case NOTNULL:
            return field.isNotNull();
        case NULL:
            return field.isNull();
        case PREFIX:
            return field.like(condition.getValue() + "%");
        case OR:
            return toCondition(field, condition.getLeft()).or(toCondition(field, condition.getRight()));
        default:
            throw new IllegalArgumentException("Invalid condition type [" + condition.getConditionType() + "]");
        }
    }

}
