package io.cattle.platform.api.resource;

import com.netflix.config.DynamicIntProperty;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.ibuildthecloud.gdapi.condition.Condition.*;

public interface ResourceManagerBaseSupport extends ResourceManager {

    String DEFAULT_CRITERIA = " _defaultCriteria";
    DynamicIntProperty REMOVE_DELAY = ArchaiusUtil.getInt("api.show.removed.for.seconds");

    Object updateObjectSupport(String type, String id, Object obj, ApiRequest request);

    Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options);

    Object deleteObjectSupport(String type, String id, Object obj, ApiRequest request);

    void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy);

    @Override
    default Object getById(String type, String id, ListOptions options) {
        Map<Object, Object> criteria = getDefaultCriteria(true, false, type);
        criteria.put(TypeUtils.ID_FIELD, id);

        return ApiUtils.getFirstFromList(listSupport(ApiContext.getSchemaFactory(), type, criteria, options));
    }

    @Override
    default Object list(String type, ApiRequest request) {
        return list(type, new LinkedHashMap<>(request.getConditions()), new ListOptions(request));
    }

    @Override
    default List<?> list(String type, Map<Object, Object> criteria, ListOptions options) {
        if (!isDefaultCriteria(criteria)) {
            criteria = mergeCriteria(criteria, getDefaultCriteria(false, false, type));
        }

        Object result = listSupport(ApiContext.getSchemaFactory(), type, criteria, options);
        return RequestUtils.toList(result);
    }

    default Map<Object, Object> mergeCriteria(Map<Object, Object> criteria, Map<Object, Object> other) {
        if (criteria == null) {
            criteria = new LinkedHashMap<>();
        }

        for (Map.Entry<Object, Object> entry : other.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            Object existing = criteria.get(key);

            if (existing instanceof List) {
                List<Object> newCondition = new ArrayList<>();
                newCondition.add(value);
                newCondition.addAll((List<?>)existing);
                criteria.put(key, newCondition);
            } else if (existing == null) {
                criteria.put(key, value);
            } else {
                criteria.put(key, Arrays.asList(value, existing));
            }
        }

        return criteria;
    }

    @Override
    default Object update(String type, String id, ApiRequest request) {
        Object object = getById(type, id, new ListOptions(request));
        if (object == null) {
            return null;
        }

        return updateObjectSupport(type, id, object, request);
    }

    @Override
    default Object delete(String type, String id, ApiRequest request) {
        Object object = getById(type, id, new ListOptions(request));
        if (object == null) {
            return null;
        }

        return deleteObjectSupport(type, id, object, request);
    }

    default Map<Object, Object> getDefaultCriteria(boolean byId, boolean byLink, String type) {
        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(DEFAULT_CRITERIA, true);

        Policy policy = ApiUtils.getPolicy();

        addAccountAuthorization(byId, byLink, type, criteria, policy);

        if (!showRemoved() && !byId) {
            /* removed is null or removed >= (NOW() - delay) */
            criteria.put(ObjectMetaDataManager.REMOVED_FIELD, isNull().or(gte(removedTime())));

            /* remove_time is null or remove_time > NOW() */
            criteria.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, isNull().or(gt(new Date())));
        }

        return criteria;
    }

    default Date removedTime() {
        return new Date(System.currentTimeMillis() - REMOVE_DELAY.get() * 1000);
    }

    default boolean showRemoved() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        if (request == null) {
            return false;
        }
        return request.getOptions().containsKey("_removed");
    }

    default boolean isDefaultCriteria(Map<Object, Object> criteria) {
        return criteria != null && criteria.containsKey(DEFAULT_CRITERIA);
    }


}
