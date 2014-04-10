package io.cattle.platform.api.resource.jooq;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractObjectResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.MapRelationship;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.meta.Relationship.RelationshipType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
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
        return listInternal(schemaFactory, type, criteria, options, null);
    }

    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options,
            Map<Table<?>,Condition> joins) {
        Class<?> clz = getClass(schemaFactory, type, criteria, true);
        if ( clz == null ) {
            return null;
        }

        type = schemaFactory.getSchemaName(clz);
        Table<?> table = JooqUtils.getTableFromRecordClass(clz);
        Sort sort = options == null ? null : options.getSort();
        Pagination pagination = options == null ? null : options.getPagination();
        Include include = options ==null ? null : options.getInclude();

        if ( table == null )
            return null;

        SelectQuery<?> query = create().selectQuery();
        MultiTableMapper mapper = addTables(schemaFactory, query, type, table, criteria, include, pagination, joins);
        addJoins(query, joins);
        addConditions(schemaFactory, query, type, table, criteria);
        addSort(schemaFactory, type, sort, query);
        addLimit(schemaFactory, type, pagination, query);

        List<?> result = mapper == null ? query.fetch() : query.fetchInto(mapper);

        processPaginationResult(result, pagination, mapper);

        return result;
    }

    protected void addJoins(SelectQuery<?> query, Map<Table<?>, Condition> joins) {
        if ( joins == null ) {
            return;
        }

        for ( Map.Entry<Table<?>, Condition> entry : joins.entrySet() ) {
            query.addJoin(entry.getKey(), JoinType.LEFT_OUTER_JOIN, entry.getValue());
        }
    }

    protected void processPaginationResult(List<?> result, Pagination pagination, MultiTableMapper mapper) {
        Integer limit = pagination == null ? null : pagination.getLimit();
        if ( limit == null ) {
            return;
        }

        long offset = getOffset(pagination);
        boolean partial = false;

        if ( mapper == null ) {
            partial = result.size() > limit;
            if ( partial ) {
                result.remove(result.size() - 1);
            }
        } else {
            partial = mapper.getResultSize() > limit;
        }

        if ( partial ) {
            Pagination paginationResponse = new Pagination(limit);
            paginationResponse.setPartial(true);
            paginationResponse.setNext(ApiContext.getUrlBuilder().next("m" + (offset + limit)));

            pagination.setResponse(paginationResponse);
        } else {
            pagination.setResponse(new Pagination(limit));
        }
    }

    protected int getOffset(Pagination pagination) {
        Object marker = getMarker(pagination);
        if ( marker == null ) {
            return 0;
        } else if ( marker instanceof String ) {
            /* Important to check that marker is a string.  If you don't then
             * somebody could use the marker functionality to deobfuscate ID's
             * and find their long value.
             */
            try {
                return Integer.parseInt((String)marker);
            } catch ( NumberFormatException nfe) {
                return 0;
            }
        }

        return 0;
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

    protected MultiTableMapper addTables(SchemaFactory schemaFactory, SelectQuery<?> query, String type, Table<?> table,
            Map<Object, Object> criteria, Include include, Pagination pagination, Map<Table<?>,Condition> joins) {
        if ( ( joins == null || joins.size() == 0 ) && (include == null || include.getLinks().size() == 0) ) {
            query.addFrom(table);
            return null;
        }

        MultiTableMapper tableMapper = new MultiTableMapper(getMetaDataManager(), pagination);
        tableMapper.map(table);

        if ( include == null ) {
            query.addSelect(tableMapper.getFields());
            query.addFrom(table);
            return tableMapper;
        }

        List<Relationship> rels = new ArrayList<Relationship>();
        rels.add(null);

        for ( Map.Entry<String, Relationship> entry : getLinkRelationships(schemaFactory, type, include).entrySet() ) {
            Relationship rel = entry.getValue();
            Table<?> childTable = JooqUtils.getTableFromRecordClass(rel.getObjectType());
            if ( childTable == null ) {
                throw new IllegalStateException("Failed to find table for type [" + rel.getObjectType() + "]");
            } else {
                String key = rel.getRelationshipType() == RelationshipType.REFERENCE ?
                        ApiUtils.SINGLE_ATTACHMENT_PREFIX + rel.getName() : rel.getName();
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
                if ( rel.getRelationshipType() == RelationshipType.MAP ) {
                    addMappingJoins(query, toTable, schemaFactory, type, table, toTable.getName(), (MapRelationship)rel);
                } else {
                    query.addJoin(toTable, JoinType.LEFT_OUTER_JOIN, getJoinCondition(schemaFactory, type, table, toTable.getName(), rel));
                }
            }
        }

        return tableMapper;
    }

    protected void addMappingJoins(SelectQuery<?> query, Table<?> toTable, SchemaFactory schemaFactory, String fromType, Table<?> from, String asName, MapRelationship rel) {
        Table<?> mappingTable = JooqUtils.getTableFromRecordClass(rel.getMappingType());
        String mappingType = schemaFactory.getSchemaName(rel.getMappingType());

        TableField<?, Object> fieldFrom = JooqUtils.getTableField(getMetaDataManager(), fromType, ObjectMetaDataManager.ID_FIELD);
        TableField<?, Object> fieldTo = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getPropertyName());
        TableField<?, Object> fieldRemoved = JooqUtils.getTableField(getMetaDataManager(), mappingType, ObjectMetaDataManager.REMOVED_FIELD);

        org.jooq.Condition cond = fieldFrom.eq(fieldTo.getTable().field(fieldTo.getName())).and(fieldRemoved == null ? DSL.trueCondition() : fieldRemoved.isNull());
        query.addJoin(mappingTable, JoinType.LEFT_OUTER_JOIN, cond);

        fieldFrom = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getOtherRelationship().getPropertyName());
        fieldTo = JooqUtils.getTableField(getMetaDataManager(), schemaFactory.getSchemaName(rel.getObjectType()), ObjectMetaDataManager.ID_FIELD);

        cond = fieldFrom.eq(fieldTo.getTable().asTable(asName).field(fieldTo.getName()));
        query.addJoin(toTable, JoinType.LEFT_OUTER_JOIN, cond);
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

    protected void addConditions(SchemaFactory schemaFactory, SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria) {
        org.jooq.Condition condition = JooqUtils.toConditions(getMetaDataManager(), type, criteria);

        if ( condition != null ) {
            query.addConditions(condition);
        }
    }

    @Override
    protected Object getMapLink(String fromType, String id, MapRelationship rel, ApiRequest request) {
        SchemaFactory schemaFactory = request.getSchemaFactory();
        /* We don't required the mapping type to be visible external, that's why we use the schemaFactory
         * from the objectManager, because it is the superset schemaFactory.
         */
        String mappingType = getObjectManager().getSchemaFactory().getSchemaName(rel.getMappingType());
        String type = schemaFactory.getSchemaName(rel.getObjectType());
        Map<Table<?>,Condition> joins = new LinkedHashMap<Table<?>, Condition>();
        Map<Object, Object> criteria = new LinkedHashMap<Object, Object>();

        if ( mappingType == null || type == null ) {
            return null;
        }

        Table<?> mappingTable = JooqUtils.getTable(schemaFactory, rel.getMappingType());

        TableField<?, Object> fieldFrom = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ID_FIELD);
        TableField<?, Object> fieldTo = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getOtherRelationship().getPropertyName());
        TableField<?, Object> fieldRemoved = JooqUtils.getTableField(getMetaDataManager(), mappingType, ObjectMetaDataManager.REMOVED_FIELD);
        TableField<?, Object> fromTypeIdField = JooqUtils.getTableField(getMetaDataManager(), mappingType, rel.getSelfRelationship().getPropertyName());

        org.jooq.Condition cond = fieldFrom.eq(fieldTo.getTable().field(fieldTo.getName()))
                                    .and(fieldRemoved == null ? DSL.trueCondition() : fieldRemoved.isNull());

        joins.put(mappingTable, cond);
        criteria.put(Condition.class, fromTypeIdField.eq(id));

        return listInternal(schemaFactory, type, criteria, new ListOptions(request), joins);
    }

    protected void addLimit(SchemaFactory schemaFactory, String type, Pagination pagination, SelectQuery<?> query) {
        if ( pagination == null || pagination.getLimit() == null ) {
            return;
        }

        int limit = pagination.getLimit() + 1;
        int offset = getOffset(pagination);
        query.addLimit(offset, limit);
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
    protected Object removeFromStore(String type, String id, Object obj, ApiRequest request) {
        Table<?> table = JooqUtils.getTableFromRecordClass(JooqUtils.getRecordClass(request.getSchemaFactory(), obj.getClass()));
        TableField<?, Object> idField = JooqUtils.getTableField(getMetaDataManager(), type, ObjectMetaDataManager.ID_FIELD);

        int result = create()
            .delete(table)
            .where(idField.eq(id))
            .execute();

        if ( result != 1 ) {
            log.error("While deleting type [{}] and id [{}] got a result of [{}]", type, id, result);
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        }

        return obj;
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
