package io.cattle.platform.api.resource.jooq;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Sort;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DefaultDSLContext;

public class JooqResourceListSupport {

    Configuration configuration;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;

    public JooqResourceListSupport(Configuration configuration, ObjectManager objectManager, ObjectMetaDataManager metaDataManager) {
        super();
        this.configuration = configuration;
        this.objectManager = objectManager;
        this.metaDataManager = metaDataManager;
    }

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Class<?> clz = getClass(schemaFactory, type, criteria, true);
        if (clz == null) {
            return null;
        }

        /* Use core schema, parent may not be authorized */
        type = objectManager.getSchemaFactory().getSchemaName(clz);
        Table<?> table = JooqUtils.getTableFromRecordClass(clz);
        Sort sort = options == null ? null : options.getSort();
        Pagination pagination = options == null ? null : options.getPagination();

        if (table == null)
            return null;

        SelectQuery<?> query = create().selectQuery();
        query.addFrom(table);
        addSort(schemaFactory, type, sort, query);

        addConditions(schemaFactory, query, type, table, criteria);
        addLimit(schemaFactory, type, pagination, query);

        List<?> result = query.fetch();
        processPaginationResult(result, pagination);

        return result;
    }

    protected void addJoins(SelectQuery<?> query, Map<Table<?>, Condition> joins) {
        if (joins == null) {
            return;
        }

        for (Map.Entry<Table<?>, Condition> entry : joins.entrySet()) {
            query.addJoin(entry.getKey(), JoinType.LEFT_OUTER_JOIN, entry.getValue());
        }
    }

    protected void processPaginationResult(List<?> result, Pagination pagination) {
        Integer limit = pagination == null ? null : pagination.getLimit();
        if (limit == null) {
            return;
        }

        long offset = getOffset(pagination);
        boolean partial = false;

        partial = result.size() > limit;
        if (partial) {
            result.remove(result.size() - 1);
        }

        if (partial) {
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
        if (marker == null) {
            return 0;
        } else if (marker instanceof String) {
            /*
             * Important to check that marker is a string. If you don't then
             * somebody could use the marker functionality to deobfuscate ID's
             * and find their long value.
             */
            try {
                return Integer.parseInt((String) marker);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }

        return 0;
    }

    protected Class<?> getClass(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, boolean alterCriteria) {
        Schema schema = schemaFactory.getSchema(type);
        Class<?> clz = schemaFactory.getSchemaClass(type);

        Schema clzSchema = schemaFactory.getSchema(clz);

        if (clz != null && (clzSchema == null || !schema.getId().equals(clzSchema.getId())) && alterCriteria) {
            criteria.put(ObjectMetaDataManager.KIND_FIELD, type);
        }

        return clz;
    }


    protected void addConditions(SchemaFactory schemaFactory, SelectQuery<?> query, String type, Table<?> table, Map<Object, Object> criteria) {
        org.jooq.Condition condition = JooqUtils.toConditions(metaDataManager, type, criteria);

        if (condition != null) {
            query.addConditions(condition);
        }
    }

    protected void addLimit(SchemaFactory schemaFactory, String type, Pagination pagination, SelectQuery<?> query) {
        if (pagination == null || pagination.getLimit() == null) {
            return;
        }

        int limit = pagination.getLimit() + 1;
        int offset = getOffset(pagination);
        query.addLimit(offset, limit);
    }

    protected void addSort(SchemaFactory schemaFactory, String type, Sort sort, SelectQuery<?> query) {
        if (sort != null) {
            TableField<?, Object> sortField = JooqUtils.getTableField(metaDataManager, type, sort.getName());
            if (sortField == null) {
                return;
            }

            switch (sort.getOrderEnum()) {
                case DESC:
                    query.addOrderBy(sortField.desc());
                    break;
                default:
                    query.addOrderBy(sortField.asc());
            }
        }

        TableField<?, Object> idSort = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.ID_FIELD);
        if (idSort == null) {
            return;
        }

        if (sort != null) {
            switch (sort.getOrderEnum()) {
                case DESC:
                    query.addOrderBy(idSort.desc());
                    break;
                default:
                    query.addOrderBy(idSort.asc());
            }
        }
        else {
            query.addOrderBy(idSort.asc());
        }
    }

    protected Object getMarker(Pagination pagination) {
        if (pagination == null) {
            return null;
        }

        String marker = pagination.getMarker();
        if (StringUtils.isBlank(marker)) {
            return null;
        }

        if (marker.charAt(0) == 'm') {
            return marker.substring(1);
        }

        Object obj = ApiContext.getContext().getIdFormatter().parseId(marker);
        if (obj instanceof Long) {
            return obj;
        } else if (obj != null) {
            try {
                return new Long(obj.toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return null;
    }

}
