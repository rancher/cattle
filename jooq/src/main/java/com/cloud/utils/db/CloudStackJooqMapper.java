
package com.cloud.utils.db;

import java.io.Serializable;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.NoOp;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.RecordType;
import org.jooq.Table;
import org.jooq.TableField;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

/*
 * This is all currently a hack
 */
public class CloudStackJooqMapper<R extends Record, E> implements RecordMapper<R, E> {

    GenericDaoBase<E, Serializable> dao;
    Field<?>[] fields;

    public CloudStackJooqMapper(RecordType<R> rowType, GenericDaoBase<E, Serializable> dao) {
        super();
        this.dao = dao;
        this.fields = rowType.fields();
    }

    @Override
    public E map(R record) {
        return toEntityBean(record);
    }

    @SuppressWarnings("unchecked") @DB()
    protected E toEntityBean(R result) {
        final E entity = (E)dao._factory.newInstance(new Callback[] {NoOp.INSTANCE, new UpdateBuilder(dao)});

        toEntityBean(result, entity);

        return entity;
    }
    
    protected void toEntityBean(final R record, final E entity) {
        for ( int i = 0 ; i < fields.length ; i++ ) {
            Table<?> table = ((TableField)fields[i]).getTable();
            setField(entity, record, table.getName(), i);
        }

        for (Attribute attr : dao._ecAttributes) {
            dao.loadCollection(entity, attr);
        }
    }
    
    protected void setField(final Object entity, final R record, String tableName, int index) {
        Attribute attr = dao._allColumns.get(new Pair<String, String>(tableName, fields[index].getName()));
        if ( attr == null ){
            // work around for mysql bug to return original table name instead of view name in db view case
            javax.persistence.Table tbl = entity.getClass().getSuperclass().getAnnotation(javax.persistence.Table.class);
            if ( tbl != null ){
                attr = dao._allColumns.get(new Pair<String, String>(tbl.name(), fields[index].getName()));
            }
        }
        try {
            if ( attr != null && attr.field != null ) {
                map(record, entity, attr.field, index);
            }
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException("Failed to map fields", e);
        }
    }

    private final void map(Record record, Object result, java.lang.reflect.Field member, int index) throws IllegalAccessException {
        Class<?> mType = member.getType();

        if (mType.isPrimitive()) {
            if (mType == byte.class) {
                member.setByte(result, record.getValue(index, byte.class));
            } else if (mType == short.class) {
                member.setShort(result, record.getValue(index, short.class));
            } else if (mType == int.class) {
                member.setInt(result, record.getValue(index, int.class));
            } else if (mType == long.class) {
                member.setLong(result, record.getValue(index, long.class));
            } else if (mType == float.class) {
                member.setFloat(result, record.getValue(index, float.class));
            } else if (mType == double.class) {
                member.setDouble(result, record.getValue(index, double.class));
            } else if (mType == boolean.class) {
                member.setBoolean(result, record.getValue(index, boolean.class));
            } else if (mType == char.class) {
                member.setChar(result, record.getValue(index, char.class));
            }
        } else {
            member.set(result, record.getValue(index, mType));
        }
    }

}
