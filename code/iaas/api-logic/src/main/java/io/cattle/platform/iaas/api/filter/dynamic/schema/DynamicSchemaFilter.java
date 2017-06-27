package io.cattle.platform.iaas.api.filter.dynamic.schema;

import static io.cattle.platform.object.meta.ObjectMetaDataManager.*;

import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;
import java.util.Map;

public class DynamicSchemaFilter extends AbstractValidationFilter {

    private static final String DEFINITION_FIELD = "definition";

    JsonMapper jsonMapper;
    LockManager lockManager;

    public DynamicSchemaFilter(JsonMapper jsonMapper, LockManager lockManager) {
        super();
        this.jsonMapper = jsonMapper;
        this.lockManager = lockManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object create(final String type, final ApiRequest request, final ResourceManager next) {
        final Map<String, Object> requestObject = (Map<String, Object>) request.getRequestObject();
        final String name = String.valueOf(requestObject.get("name"));
        if (requestObject.get(ACCOUNT_FIELD) == null) {
            requestObject.put(ACCOUNT_FIELD, null);
        }
        final Long accountId = requestObject.get(ACCOUNT_FIELD) == null ? null : Long.valueOf(String.valueOf(requestObject.get(ACCOUNT_FIELD)));
        final List<String> roles = (List<String>) requestObject.get("roles");
        return lockManager.lock(new DynamicSchemaFilterLock(name), new LockCallback<Object>() {

            @Override
            public Object doWithLock() {
                if ((roles == null || roles.isEmpty()) && accountId == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "MustSpecifyAccountIdOrRole");
                }
                try {
                    jsonMapper.readValue(
                            String.valueOf(requestObject.get(DEFINITION_FIELD)).getBytes("UTF-8"), SchemaImpl.class);
                } catch (Exception e) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_FORMAT, DEFINITION_FIELD);
                }
                return DynamicSchemaFilter.super.create(type, request, next);
            }
        });
    }

}
