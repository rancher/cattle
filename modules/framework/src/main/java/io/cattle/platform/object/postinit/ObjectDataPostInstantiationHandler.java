package io.cattle.platform.object.postinit;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ObjectDataPostInstantiationHandler implements ObjectPostInstantiationHandler {

    JsonMapper jsonMapper;

    public ObjectDataPostInstantiationHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        if (!hasDataField(obj))
            return obj;

        try {
            Map<String, Object> data = getData(obj, properties);
            setData(obj, data);
            return obj;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to handle object data for [" + obj + "] and properties [" + properties + "]", e);
        }
    }

    protected boolean hasDataField(Object obj) {
        PropertyDescriptor desc;
        try {
            desc = PropertyUtils.getPropertyDescriptor(obj, "data");
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }

        return !(desc == null || desc.getReadMethod() == null || desc.getPropertyType() != Map.class);
    }

    protected void setData(Object instance, Map<String, Object> data) throws IOException {
        try {
            BeanUtils.setProperty(instance, DataAccessor.DATA, data);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set data [" + data + "] on [" + instance + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to set data [" + data + "] on [" + instance + "]", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getData(Object instance, Map<String, Object> properties) throws IOException {
        Map<String, Object> objectData = null;
        Map<String, Object> inputData = getMap(properties.get(DataAccessor.DATA));

        try {
            objectData = (Map<String, Object>) PropertyUtils.getProperty(instance, DataAccessor.DATA);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }

        Map<String, Object> finalData = new TreeMap<>();

        if (objectData != null) {
            finalData.putAll(objectData);
        }
        finalData.putAll(inputData);

        overlay(finalData, DataAccessor.FIELDS, getFieldData(instance, properties));

        return finalData;
    }

    protected Map<String, Object> getFieldData(Object instance, Map<String, Object> properties) {
        Map<String, Object> fields = new HashMap<>();
        for (String name : properties.keySet()) {
            try {
                if (PropertyUtils.getPropertyDescriptor(instance, name) != null)
                    continue;
            } catch (IllegalAccessException e) {
                continue;
            } catch (InvocationTargetException e) {
                continue;
            } catch (NoSuchMethodException e) {
                // This exception would be okay, but it never seems to happen
            }

            fields.put(name, properties.get(name));
        }

        return fields;
    }

    protected void overlay(Map<String, Object> data, String key, Map<String, Object> overlay) {
        Map<String, Object> existing = getMap(data.get(key));
        existing.putAll(overlay);
        if (existing.size() == 0) {
            data.remove(key);
        } else {
            data.put(key, existing);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

}
