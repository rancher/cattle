package io.github.ibuildthecloud.dstack.api.resource.jooq;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.resource.AbstractObjectResourceManager;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.meta.Relationship.RelationshipType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.model.Pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Condition;
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
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Class<?> clz = getClass(schemaFactory, type, criteria, true);
        if ( clz == null ) {
            return null;
        }

        type = schemaFactory.getSchemaName(clz);
        Table<?> table = JooqUtils.getTable(clz);
        Sort sort = options == null ? null : options.getSort();
        Pagination pagination = options == null ? null : options.getPagination();
        Include include = options ==null ? null : options.getInclude();
        Long marker = getMarker(pagination);

        if ( table == null )
            return null;

        SelectQuery<?> query = create().selectQuery();
        MultiTableMapper mapper = addTables(schemaFactory, query, type, table, criteria, include);
        addConditions(schemaFactory, query, type, table, criteria, marker);
        addSort(schemaFactory, type, sort, query);
        addLimit(schemaFactory, type, pagination, query);

        return mapper == null ? query.fetch() : query.fetchInto(mapper);
    }

    protected Class<?> getClass(SchemaFactory schemaFactory, String type, Map<Object,Object> criteria, boolean alterCriteria) {
        Schema schema = schemaFactory.getSchema(type);
        Class<?> clz = schemaFactory.getSchemaClass(type);

        if ( clz == null && schema.getParent() != null ) {
            clz = getClass(schemaFactory, schema.getParent(), criteria, false);
            if ( clz != null && alterCriteria ) {
                criteria.put(ObjectMetaDataManager.KIND_FIELD, type);
            }
        }

        return clz;
    }

    protected MultiTableMapper addTables(SchemaFactory schemaFactory, SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria, Include include) {
        if ( include == null || include.getLinks().size() == 0 ) {
            query.addFrom(table);
            return null;
        }

        MultiTableMapper tableMapper = new MultiTableMapper(getMetaDataManager());
        tableMapper.map(table);

        List<Relationship> rels = new ArrayList<Relationship>();
        rels.add(null);

        for ( Map.Entry<String, Relationship> entry : getLinkRelationships(schemaFactory, type, include).entrySet() ) {
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
                query.addJoin(toTable, getJoinCondition(schemaFactory, type, table, toTable.getName(), rel));
            }
        }

        return tableMapper;
    }

    protected org.jooq.Condition getJoinCondition(SchemaFactory schemaFactory, String fromType, Table<?> from, String asName, Relationship rel) {
        TableField<?, Object> fieldFrom = null;
        TableField<?, Object> fieldTo = null;

        switch(rel.getRelationshipType()) {
        case REFERENCE:
            fieldFrom = JooqUtils.getTableField(getMetaDataManager(), fromType, rel.getPropertyName());
            fieldTo = JooqUtils.getTableField(getMetaDataManager(), schemaFactory.getSchemaName(rel.getObjectType()),
                    ObjectMetaDataManager.ID_FIELD);
            break;
        case CHILD:
            fieldFrom = JooqUtils.getTableField(getMetaDataManager(), fromType, ObjectMetaDataManager.ID_FIELD);
            fieldTo = JooqUtils.getTableField(getMetaDataManager(), schemaFactory.getSchemaName(rel.getObjectType()), rel.getPropertyName());
            break;
        default:
            throw new IllegalArgumentException("Illegal Relationship type [" + rel.getRelationshipType() + "]");
        }

        if ( fieldFrom == null || fieldTo == null ) {
            throw new IllegalStateException("Failed to construction join query for [" + fromType + "] [" + from + "] [" + rel + "]");
        }

        return fieldFrom.eq(fieldTo.getTable().as(asName).field(fieldTo.getName()));
    }

    protected void addConditions(SchemaFactory schemaFactory, SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria, Long marker) {
        org.jooq.Condition condition = JooqUtils.toConditions(getMetaDataManager(), type, criteria);

        condition = addMarker(type, marker, condition);

        if ( condition != null ) {
            query.addConditions(condition);
        }
    }

    protected void addLimit(SchemaFactory schemaFactory, String type, Pagination pagination, SelectQuery<?> query) {
        if ( pagination == null ) {
            return;
        }

        int limit = pagination.getLimit() + 1;
        query.addLimit(limit);
    }

    protected void addSort(SchemaFactory schemaFactory, String type, Sort sort, SelectQuery<?> query) {
        if ( sort == null ) {
            return;
        }

        TableField<?, Object> sortField = JooqUtils.getTableField(getMetaDataManager(), type, sort.getName());
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

        TableField<?, Object> field = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ID_FIELD);
        if ( field != null ) {
            org.jooq.Condition newCondition = field.ge(marker);
            return existing == null ? newCondition : existing.and(newCondition);
        }

        return existing;
    }

    @Override
    protected void addAccountAuthorization(String type, Map<Object, Object> criteria, Policy policy) {
        super.addAccountAuthorization(type, criteria, policy);

        if ( ! policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) ) {
            TableField<?, Object> accountField = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ACCOUNT_FIELD);
            TableField<?, Object> publicField = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.PUBLIC_FIELD);
            Object accountValue = criteria.get(ObjectMetaDataManager.ACCOUNT_FIELD);

            if ( accountField == null || publicField == null || accountValue == null ) {
                return;
            }

            criteria.remove(ObjectMetaDataManager.ACCOUNT_FIELD);
            Condition accountCondition = null;
            if ( accountValue instanceof io.github.ibuildthecloud.gdapi.condition.Condition ) {
                accountCondition = accountField.in(((io.github.ibuildthecloud.gdapi.condition.Condition)accountValue).getValues());
            } else {
                accountCondition = accountField.eq(accountValue);
            }

            criteria.put(Condition.class, publicField.isTrue().or(accountCondition));
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
