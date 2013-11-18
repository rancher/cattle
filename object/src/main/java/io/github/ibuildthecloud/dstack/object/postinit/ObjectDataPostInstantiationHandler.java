package io.github.ibuildthecloud.dstack.object.postinit;

import io.github.ibuildthecloud.dstack.json.JsonMapper;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

public class ObjectDataPostInstantiationHandler implements ObjectPostInstantiationHandler {

    public static final String DATA = "data";
    public static final String OPTIONS = "options";
    public static final String FIELDS = "fields";

    JsonMapper jsonMapper;

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String,Object> properties) {
        if ( ! hasDataField(obj) )
            return obj;

        try {
            Map<String,Object> data = getData(obj, properties);
            setData(obj, data);
            return obj;
        } catch ( IOException e ) {
            throw new IllegalStateException("Failed to handle object data for [" + obj + 
                    "] and properties [" + properties + "]", e);
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

        if ( desc == null || desc.getReadMethod() == null || desc.getPropertyType() != String.class ) {
            return false;
        }

        return true;
    }

    protected void setData(Object instance, Map<String,Object> data) throws IOException {
        String stringData = jsonMapper.writeValueAsString(data);
        try {
            BeanUtils.setProperty(instance, DATA, stringData);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set data [" + stringData + "] on [" + instance + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to set data [" + stringData + "] on [" + instance + "]", e);
        }
    }

    protected Map<String,Object> getData(Object instance, Map<String,Object> properties) throws IOException {
        String objectData = null;
        Map<String,Object> inputData = getMap(properties.get(DATA));

        try {
            objectData = BeanUtils.getProperty(instance, DATA);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }

        Map<String,Object> finalData = new TreeMap<String, Object>();

        if ( objectData != null ) {
            Map<String,Object> existingData = jsonMapper.readValue(objectData);
            finalData.putAll(existingData);
        }

        finalData.putAll(inputData);

        overlay(finalData, OPTIONS, getMap(inputData.get(OPTIONS)));
        overlay(finalData, FIELDS, getFieldData(instance, properties));

        return finalData;
    }

    protected Map<String,Object> getFieldData(Object instance, Map<String,Object> properties) {
        Map<String,Object> fields = new HashMap<String, Object>();
        for ( String name : properties.keySet() ) {
            try {
                if ( PropertyUtils.getPropertyDescriptor(instance, name) != null )
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

    protected void overlay(Map<String,Object> data, String key, Map<String,Object> overlay) {
        Map<String,Object> existing = getMap(data.get(key));
        existing.putAll(overlay);
        if ( existing.size() == 0 ) {
            data.remove(key);
        } else {
            data.put(key, existing);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String,Object> getMap(Object obj) {
        if ( obj instanceof Map ) {
            return (Map<String,Object>)obj;
        }
        return new HashMap<String, Object>();
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
