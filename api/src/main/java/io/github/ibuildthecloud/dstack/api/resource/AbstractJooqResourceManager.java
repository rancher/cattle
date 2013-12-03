package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DefaultDSLContext;

public abstract class AbstractJooqResourceManager extends AbstractResourceManager {

//    private static final Logger log = LoggerFactory.getLogger(AbstractJooqResourceManager.class);

    Configuration configuration;

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        Table<?> table = JooqUtils.getTable(clz);

        if ( table == null )
            return null;

        org.jooq.Condition condition = toConditions(type, criteria);
        if ( condition == null ) {
            return create().selectFrom(table).fetch();
        } else {
            return create().selectFrom(table).where(condition).fetch();
        }
    }

    protected org.jooq.Condition toConditions(String type, Map<Object, Object> criteria) {
        org.jooq.Condition existingCondition = null;

        for ( Map.Entry<Object, Object> entry : criteria.entrySet() ) {
            Object objField = metaDataManager.convertFieldNameFor(type, entry.getKey());
            Object value = entry.getValue();
            if ( ! ( objField instanceof TableField )) {
                continue;
            }
            @SuppressWarnings("unchecked")
            TableField<?, Object> field = (TableField<?, Object>)objField;
            org.jooq.Condition newCondition = null;

            if ( value instanceof Condition ) {
                newCondition = toCondition(field, (Condition)value);
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

    protected org.jooq.Condition toCondition(TableField<?, Object> field, Condition value) {
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

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


}
