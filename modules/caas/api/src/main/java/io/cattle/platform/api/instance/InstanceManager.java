package io.cattle.platform.api.instance;

import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.revision.RevisionManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.impl.ValidationErrorImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstanceManager extends DefaultResourceManager {

    RevisionManager revisionManager;
    ReferenceValidator validator;

    public InstanceManager(DefaultResourceManagerSupport support, RevisionManager revisionManager, ReferenceValidator validator) {
        super(support);
        this.revisionManager = revisionManager;
        this.validator = validator;
    }

    @Override
    public Object deleteObject(String type, String id, Object obj, ApiRequest request) {
        if (!(obj instanceof Instance)) {
            return super.deleteObject(type, id, obj, request);
        }

        try {
            return super.deleteObject(type, id, obj, request);
        } catch (ClientVisibleException e) {
            if (ResponseCodes.METHOD_NOT_ALLOWED == e.getStatus() ) {
                objectResourceManagerSupport.getObjectProcessManager().stopThenRemove(obj, null);
                return objectResourceManagerSupport.getObjectManager().reload(obj);
            } else {
                throw e;
            }
        }
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Map<String, Object> properties = CollectionUtils.toMap(request.getRequestObject());
        Object count = properties.get(InstanceConstants.FIELD_COUNT);

        if (count instanceof Number && ((Number) count).intValue() > 1) {
            int max = Integer.min(((Number) count).intValue(), 20);

            List<Object> result = new ArrayList<>(max);
            String baseName = null;
            for (int i = 0; i < max; i++) {
                if (baseName != null) {
                    String name = baseName + "-" + i;
                    Object obj = validator.getByField(InstanceConstants.TYPE, ObjectMetaDataManager.NAME_FIELD, name, null);
                    if (obj != null) {
                        error(ValidationErrorCodes.NOT_UNIQUE, ObjectMetaDataManager.NAME_FIELD);
                    }
                    properties.put(ObjectMetaDataManager.NAME_FIELD, name);
                }

                createRevision(properties);
                request.setRequestObject(properties);
                Object instance = super.create(type, request);
                result.add(instance);

                if (baseName == null && instance instanceof Instance) {
                    baseName = ((Instance) instance).getName();
                }
            }

            return result;
        } else {
            createRevision(properties);
            return super.create(type, request);
        }
    }

    protected void createRevision(Map<String, Object> properties) {
        Long accountId = DataAccessor.fromMap(properties).withKey(ObjectMetaDataManager.ACCOUNT_FIELD).as(Long.class);
        if (accountId == null) {
            accountId = ApiUtils.getPolicy().getAccountId();
        }

        revisionManager.createInitialRevisionForInstance(accountId, properties);
    }

    protected static Object error(String code, String fieldName) {
        ValidationErrorImpl error = new ValidationErrorImpl(code, fieldName);
        throw new ClientVisibleException(error);
    }


}
