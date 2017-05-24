package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.impl.ValidationErrorImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class InstanceManager extends AbstractJooqResourceManager {

    @Inject
    RevisionManager revisionManager;
    @Inject
    ReferenceValidator validator;

    @Override
    public String[] getTypes() {
        return new String[] { "instance", "container", "virtualMachine" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        if (!(obj instanceof Instance)) {
            return super.deleteInternal(type, id, obj, request);
        }

        try {
            return super.deleteInternal(type, id, obj, request);
        } catch (ClientVisibleException e) {
            if (ResponseCodes.METHOD_NOT_ALLOWED == e.getStatus() ) {
                getObjectProcessManager().stopAndRemove(obj, null);
                return getObjectManager().reload(obj);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
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
                Instance instance = super.createAndScheduleObject(Instance.class, properties);
                result.add(instance);

                if (baseName == null) {
                    baseName = instance.getName();
                }
            }

            return (T) result;
        } else {
            createRevision(properties);
            return super.createAndScheduleObject(clz, properties);
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
