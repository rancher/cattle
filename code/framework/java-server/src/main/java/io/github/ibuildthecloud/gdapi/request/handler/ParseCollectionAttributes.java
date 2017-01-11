package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Filter;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.model.Sort.SortOrder;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.validation.ValidationHandler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ParseCollectionAttributes extends AbstractApiRequestHandler {

    Integer defaultLimit = 100;
    Integer maxLimit = 3000;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (!Schema.Method.GET.isMethod(request.getMethod())) {
            return;
        }

        Schema schema = request.getSchemaFactory().getSchema(request.getType());
        if (schema == null) {
            return;
        }

        Map<String, Object> params = RequestUtils.toMap(request.getRequestObject());
        parseSort(schema, params, request);
        parsePagination(schema, params, request);
        parseFilters(schema, params, request);
        parseIncludes(schema, params, request);
    }

    protected void parseIncludes(Schema schema, Map<String, Object> params, ApiRequest request) {
        List<String> links = new ArrayList<String>();
        List<?> inputs = RequestUtils.toList(params.get(Collection.INCLUDE));
        for (Object input : inputs) {
            links.add(input.toString());
        }

        if (links.size() > 0) {
            request.setInclude(new Include(links.subList(0, 1)));
        }
    }

    protected void parseFilters(Schema schema, Map<String, Object> params, ApiRequest request) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        Map<String, Filter> filters = schema.getCollectionFilters();
        Map<String, Field> fields = schema.getResourceFields();
        Map<String, List<Condition>> conditions = new TreeMap<String, List<Condition>>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            NameAndOp nameAndOp = new NameAndOp(entry.getKey());
            Filter filter = filters.get(nameAndOp.getName());
            Field field = fields.get(nameAndOp.getName());
            if (filter == null || field == null) {
                continue;
            }

            for (String mod : filter.getModifiers()) {
                if (nameAndOp.getOp().equals(mod)) {
                    List<Condition> conditionList = new ArrayList<Condition>();
                    ConditionType conditionType = ConditionType.valueOf(nameAndOp.getOp().toUpperCase());

                    for (Object obj : RequestUtils.toList(entry.getValue())) {
                        if (field.getTypeEnum() == FieldType.REFERENCE) {
                            obj = formatter.parseId(obj.toString());
                            if (obj == null) {
                                obj = "-1";
                            }
                        } else if (field.getTypeEnum() == FieldType.DATE) {
                            try {
                                obj = ValidationHandler.convertDate(field.getName(), obj);
                            } catch (ClientVisibleException e) {
                            }
                        }
                        conditionList.add(new Condition(conditionType, obj));
                    }

                    conditions.put(nameAndOp.getName(), conditionList);
                }
            }
        }

        request.setConditions(conditions);
    }

    protected void parseSort(Schema schema, Map<String, Object> params, ApiRequest request) {
        SortOrder orderEnum = SortOrder.ASC;
        String sort = RequestUtils.getSingularStringValue(Collection.SORT, params);
        String order = RequestUtils.getSingularStringValue(Collection.ORDER, params);
        if (order != null) {
            try {
                orderEnum = SortOrder.valueOf(order.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Field field = schema.getResourceFields().get(sort);
        if (field == null || !schema.getCollectionFilters().containsKey(sort)) {
            return;
        }

        URL reverseUrl = ApiContext.getUrlBuilder().reverseSort(orderEnum);
        if (reverseUrl == null) {
            return;
        }

        request.setSort(new Sort(sort, orderEnum, reverseUrl));
    }

    protected void parsePagination(Schema schema, Map<String, Object> params, ApiRequest request) {
        String limit = RequestUtils.getSingularStringValue(Collection.LIMIT, params);
        String marker = RequestUtils.getSingularStringValue(Collection.MARKER, params);
        Pagination pagination = new Pagination(defaultLimit == null ? maxLimit : defaultLimit);
        pagination.setMarker(marker);

        try {
            if (limit != null) {
                Integer limitInt = new Integer(limit);
                if (maxLimit != null && limitInt.intValue() > maxLimit.intValue()) {
                    limitInt = maxLimit;
                }
                if (limitInt != null && limitInt.intValue() <= 0) {
                    if (maxLimit == null) {
                        pagination.setLimit(null);
                    } else {
                        pagination.setLimit(maxLimit);
                    }
                } else {
                    pagination.setLimit(limitInt);
                }
            }
        } catch (NumberFormatException e) {
            // ignore
        }

        request.setPagination(pagination);
    }

    public Integer getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(Integer defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Integer getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(Integer maxLimit) {
        this.maxLimit = maxLimit;
    }

    private static final class NameAndOp {
        String name;
        String op;

        public NameAndOp(String value) {
            int idx = value.lastIndexOf("_");
            if (idx == -1) {
                this.name = value;
                this.op = ConditionType.EQ.getExternalForm();
            } else {
                this.op = value.substring(idx + 1);
                this.name = value.substring(0, idx);
            }
        }

        public String getName() {
            return name;
        }

        public String getOp() {
            return op;
        }
    }

}
