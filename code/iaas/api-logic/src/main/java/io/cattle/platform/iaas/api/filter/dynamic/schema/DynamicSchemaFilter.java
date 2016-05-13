package io.cattle.platform.iaas.api.filter.dynamic.schema;

import static io.cattle.platform.object.meta.ObjectMetaDataManager.*;

import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class DynamicSchemaFilter extends AbstractResourceManagerFilter {

    private static final String DEFINITION_FIELD = "definition";

    @Inject
    JsonMapper jsonMapper;
    @Inject
    LockManager lockManager;

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
        final DynamicSchemaFilter filter = this;
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
                return filter.callSuperCreate(type, request, next);
            }
        });

    }

    private Object callSuperCreate(final String type, final ApiRequest request, final ResourceManager next) {
        return super.create(type, request, next);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { DynamicSchema.class };
    }
}
