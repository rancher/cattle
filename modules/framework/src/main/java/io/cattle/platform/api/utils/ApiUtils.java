package io.cattle.platform.api.utils;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.auth.impl.DefaultPolicy;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

public class ApiUtils {

    private static final Policy DEFAULT_POLICY = new DefaultPolicy();
    private static final Set<String> PRIORITY_FIELDS = new LinkedHashSet<>(Arrays.asList(ObjectMetaDataManager.NAME_FIELD, ObjectMetaDataManager.STATE_FIELD));

    public static Object getFirstFromList(Object obj) {
        if (obj instanceof Collection) {
            return getFirstFromList(((Collection) obj).getData());
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.size() > 0 ? list.get(0) : null;
        }

        return null;
    }

    public static Policy getPolicy() {
        Object policy = ApiContext.getContext().getPolicy();
        if (policy instanceof Policy) {
            return (Policy) policy;
        } else {
            return DEFAULT_POLICY;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T filterAuthorized(T obj) {
        Policy policy = getPolicy();
        if (obj instanceof List) {
            return (T) ((List<T>) obj).stream()
                    .filter((x) -> policy.checkAuthorized(x) != null)
                    .collect(toList());
        }
        return policy.checkAuthorized(obj);
    }

    public static Resource createResource(final ApiRequest request, final IdFormatter idFormatter,
            final SchemaFactory schemaFactory, final Schema schema, Object obj, Map<String, Object> inputAdditionalFields) {
        Map<String, Object> additionalFields = new LinkedHashMap<>();
        additionalFields.putAll(DataAccessor.getFields(obj));

        if (inputAdditionalFields != null && inputAdditionalFields.size() > 0) {
            additionalFields.putAll(inputAdditionalFields);
        }

        String method = request == null ? null : request.getMethod();
        return new WrappedResource(idFormatter, schemaFactory, schema, obj, additionalFields, PRIORITY_FIELDS, method);
    }

    public static String getSchemaIdForDisplay(SchemaFactory factory, Object obj) {
        Schema schema = factory.getSchema(obj.getClass());

        if (schema == null) {
            return null;
        }

        String kind = ObjectUtils.getKind(obj);
        Schema kindSchema = factory.getSchema(kind);
        if (kindSchema != null && schema.getId().equals(factory.getBaseType(kindSchema.getParent()))) {
            return kindSchema.getId();
        }

        return schema.getId();
    }

}
