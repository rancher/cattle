package io.github.ibuildthecloud.dstack.db.jooq.utils;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqUtils {

    private static final Logger log = LoggerFactory.getLogger(JooqUtils.class);

    public static <T> T findById(DSLContext context, Class<T> clz, Object id) {
        Table<?> table = getTable(clz);
        if ( table == null )
            return null;

        UniqueKey<?> key = table.getPrimaryKey();
        if ( key == null || key.getFieldsArray().length != 1 )
            return null;

        @SuppressWarnings("unchecked")
        TableField<?, Object> keyField = (TableField<?, Object>)key.getFieldsArray()[0];

        /* Convert object because we are abusing type safety here */
        Object converted = keyField.getDataType().convert(id);

        return context.select()
                .from(table)
                .where(keyField.eq(converted))
                .fetchOneInto(clz);
    }
    
    @SuppressWarnings("unchecked")
    public static Table<?> getTable(Class<?> clz) {
        if ( UpdatableRecord.class.isAssignableFrom(clz) ) {
            try {
                UpdatableRecord<?> record =
                        ((Class<UpdatableRecord<?>>)clz).newInstance();
                return record.getTable();
            } catch (InstantiationException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            } catch (IllegalAccessException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            }
        }
        return null;
    }
}
