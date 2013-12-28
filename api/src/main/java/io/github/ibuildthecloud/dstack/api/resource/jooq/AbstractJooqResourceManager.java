package io.github.ibuildthecloud.dstack.api.resource.jooq;

import io.github.ibuildthecloud.dstack.api.resource.AbstractObjectResourceManager;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.meta.Relationship.RelationshipType;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.model.Pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DefaultDSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJooqResourceManager extends AbstractObjectResourceManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractJooqResourceManager.class);

    Configuration configuration;

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria, ListOptions options) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        Table<?> table = JooqUtils.getTable(clz);
        Sort sort = options == null ? null : options.getSort();
        Pagination pagination = options == null ? null : options.getPagination();
        Include include = options ==null ? null : options.getInclude();
        Long marker = getMarker(pagination);

        if ( table == null )
            return null;

        SelectQuery<?> query = create().selectQuery();
        MultiTableMapper mapper = addTables(query, type, table, criteria, include);
        addConditions(query, type, table, criteria, marker);
        addSort(type, sort, query);
        addLimit(type, pagination, query);

        return mapper == null ? query.fetch() : query.fetchInto(mapper);
    }

    protected MultiTableMapper addTables(SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria, Include include) {
        if ( include == null || include.getLinks().size() == 0 ) {
            query.addFrom(table);
            return null;
        }

        MultiTableMapper tableMapper = new MultiTableMapper(getMetaDataManager());
        tableMapper.map(table);

        List<Relationship> rels = new ArrayList<Relationship>();
        rels.add(null);

        for ( Map.Entry<String, Relationship> entry : getLinkRelationships(type, include).entrySet() ) {
            Relationship rel = entry.getValue();
            Table<?> childTable = JooqUtils.getTable(rel.getObjectType());
            if ( childTable == null ) {
                throw new IllegalStateException("Failed to find table for type [" + rel.getObjectType() + "]");
            } else {
                String key = rel.getRelationshipType() == RelationshipType.REFERENCE ?
                        ApiUtils.SINGLE_ATTACHMENT_PREFIX + entry.getKey() : entry.getKey();
                tableMapper.map(key, childTable);
                rels.add(rel);
            }
        }

        List<Table<?>> tables = tableMapper.getTables();

        query.addSelect(tableMapper.getFields());
        query.addFrom(table);

        for ( int i = 0 ; i < tables.size() ; i++ ) {
            Relationship rel = rels.get(i);
            Table<?> toTable = tables.get(i);
            if ( rel != null ) {
                query.addJoin(toTable, getJoinCondition(type, table, toTable.getName(), rel));
            }
        }

        return tableMapper;
    }

    protected org.jooq.Condition getJoinCondition(String fromType, Table<?> from, String asName, Relationship rel) {
        TableField<?, Object> fieldFrom = null;
        TableField<?, Object> fieldTo = null;

        switch(rel.getRelationshipType()) {
        case REFERENCE:
            fieldFrom = getTableField(fromType, rel.getPropertyName());
            fieldTo = getTableField(schemaFactory.getSchemaName(rel.getObjectType()),
                    ObjectMetaDataManager.ID_FIELD);
            break;
        case CHILD:
            fieldFrom = getTableField(fromType, ObjectMetaDataManager.ID_FIELD);
            fieldTo = getTableField(schemaFactory.getSchemaName(rel.getObjectType()), rel.getPropertyName());
            break;
        default:
            throw new IllegalArgumentException("Illegal Relationship type [" + rel.getRelationshipType() + "]");
        }

        if ( fieldFrom == null || fieldTo == null ) {
            throw new IllegalStateException("Failed to construction join query for [" + fromType + "] [" + from + "] [" + rel + "]");
        }

        return fieldFrom.eq(fieldTo.getTable().as(asName).field(fieldTo.getName()));
    }

    protected void addConditions(SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria, Long marker) {
        org.jooq.Condition condition = toConditions(type, criteria);

        condition = addMarker(type, marker, condition);

        if ( condition != null ) {
            query.addConditions(condition);
        }
    }

    protected void addLimit(String type, Pagination pagination, SelectQuery<?> query) {
        if ( pagination == null ) {
            return;
        }

        int limit = pagination.getLimit() + 1;
        query.addLimit(limit);
    }

    protected void addSort(String type, Sort sort, SelectQuery<?> query) {
        if ( sort == null ) {
            return;
        }

        TableField<?, Object> sortField = getTableField(type, sort.getName());
        if ( sortField == null ) {
            return;
        }

        switch (sort.getOrderEnum()) {
        case DESC:
            query.addOrderBy(sortField.desc());
        default:
            query.addOrderBy(sortField.asc());
        }
    }

    protected org.jooq.Condition addMarker(String type, Long marker, org.jooq.Condition existing) {
        if ( marker == null ) {
            return existing;
        }

        TableField<?, Object> field = getTableField(type, ObjectMetaDataManager.ID_FIELD);
        if ( field != null ) {
            org.jooq.Condition newCondition = field.ge(marker);
            return existing == null ? newCondition : existing.and(newCondition);
        }

        return existing;
    }

    protected org.jooq.Condition toConditions(String type, Map<Object, Object> criteria) {
        org.jooq.Condition existingCondition = null;

        for ( Map.Entry<Object, Object> entry : criteria.entrySet() ) {
            Object value = entry.getValue();
            TableField<?, Object> field = getTableField(type, entry.getKey());
            if ( field == null ) {
                continue;
            }
            org.jooq.Condition newCondition = null;

            if ( value instanceof Condition ) {
                newCondition = toCondition(field, (Condition)value);
            } else if ( value instanceof List ) {
                newCondition = listToCondition(field, (List<?>)value);
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

    protected org.jooq.Condition listToCondition(TableField<?, Object> field, List<?> list) {
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

    @SuppressWarnings("unchecked")
    protected TableField<?, Object> getTableField(String type, Object key) {
        Object objField = getMetaDataManager().convertFieldNameFor(type, key);
        if ( objField instanceof TableField ) {
            return (TableField<?, Object>)objField;
        } else {
            return null;
        }
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

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleException(Throwable t, ApiRequest apiRequest) {
        if ( t instanceof DataAccessException ) {
            log.error("Database error", t);
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        }
        return super.handleException(t, apiRequest);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
