package io.cattle.platform.api.resource;

import io.cattle.platform.api.handler.ResponseObjectConverter;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ObjectResourceManagerSupport {

    private static final String SCHEDULE_UPDATE = "scheduleUpdate";
    private static final IdFormatter IDENTITY_FORMATTER = new IdentityFormatter();

    protected ObjectManager objectManager;
    protected ObjectProcessManager objectProcessManager;
    ResponseObjectConverter objectConverter;

    public ObjectResourceManagerSupport(ObjectManager objectManager, ObjectProcessManager objectProcessManager) {
        this.objectManager = objectManager;
        this.objectProcessManager = objectProcessManager;
    }

    public Object create(String type, ApiRequest request) {
        Class<?> clz = getClassForType(request.getSchemaFactory(), type);
        if (clz == null) {
            return null;
        }

        return doCreate(type, clz, CollectionUtils.toMap(request.getRequestObject()));
    }

    protected <T> T doCreate(String type, Class<T> clz, Map<Object, Object> data) {
        Map<String, Object> properties = objectManager.convertToPropertiesFor(clz, data);
        if (!properties.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            properties.put(ObjectMetaDataManager.KIND_FIELD, type);
        }

        return createAndScheduleObject(clz, properties);
    }

    @SuppressWarnings("unchecked")
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
        Object result = objectManager.create(clz, properties);
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, result, properties);
            result = objectManager.reload(result);
        } catch (ProcessNotFoundException e) {
        }

        return (T) result;
    }

    protected Class<?> getClassForType(SchemaFactory schemaFactory, String type) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if (clz == null) {
            Schema schema = schemaFactory.getSchema(type);
            if (schema != null && schema.getParent() != null) {
                return getClassForType(schemaFactory, schema.getParent());
            }
        }

        return clz;
    }

    public Object deleteObject(String type, String id, Object obj, ApiRequest request) {
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, obj, null);
            return objectManager.reload(obj);
        } catch (ProcessCancelException e) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
        } catch (ProcessNotFoundException e) {
            return removeFromStore(type, id, obj, request);
        }
    }

    protected Object removeFromStore(String type, String id, Object obj, ApiRequest request) {
        objectManager.delete(obj);
        return obj;
    }

    public Object updateObject(String type, String id, Object obj, ApiRequest request) {
        Map<String, Object> updates = CollectionUtils.toMap(request.getRequestObject());
        Map<String, Object> existingValues = new HashMap<>();
        Map<String, Object> filteredUpdates = new HashMap<>();
        Map<String, Object> existing = objectConverter.createResource(obj, IDENTITY_FORMATTER, request).getFields();
        Schema schema = request.getSchemaFactory().getSchema(type);
        Map<String, Field> fields = schema.getResourceFields();

        boolean schedule = false;
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object existingValue = existing.get(key);
            if (!Objects.equals(existingValue, entry.getValue())) {
                filteredUpdates.put(key, entry.getValue());
                existingValues.put(key, existingValue);
                Field field = fields.get(key);
                if (field != null) {
                    schedule |= Boolean.TRUE.equals(field.getAttributes().get(SCHEDULE_UPDATE));
                }
            }

        }

        Object result = objectManager.setFields(schema, obj, filteredUpdates);
        if (schedule) {
            filteredUpdates.put("old", existingValues);
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, filteredUpdates);
            result = objectManager.reload(result);
        }

        return result;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    public ObjectProcessManager getObjectProcessManager() {
        return objectProcessManager;
    }

    public ResponseObjectConverter getObjectConverter() {
        return objectConverter;
    }

}
