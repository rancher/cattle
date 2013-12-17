package io.github.ibuildthecloud.dstack.schema.processor;

import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonFileOverlayPostProcessor extends AbstractSchemaPostProcessor implements SchemaPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(JsonFileOverlayPostProcessor.class);

    public static final String REMOVE = "-";

    JsonMapper jsonMapper;
    io.github.ibuildthecloud.gdapi.json.JsonMapper schemaMashaller;
    boolean explicitByDefault = false;

    String path;
    String overridePath;

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        InputStream is = loadJsonFile(overridePath, schema);
        if ( is == null ) {
            is = loadJsonFile(path, schema);
        }

        if ( is == null ) {
            return schema;
        }

        try {
            byte[] bytes = IOUtils.toByteArray(is);

            Map<String,Object> mapData = jsonMapper.readValue(bytes);
            SchemaOverlayImpl data = schemaMashaller.readValue(bytes, explicitByDefault ? ExplicitByDefaultSchemaOverlayImpl.class :
                SchemaOverlayImpl.class);

            processSchema(schema, data, mapData);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return schema;
    }

    protected void processSchema(SchemaImpl schema, SchemaOverlayImpl data, Map<String, Object> mapData) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        for ( PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(schema) ) {
            String name = prop.getName();
            Method writeMethod = prop.getWriteMethod();
            if ( writeMethod == null || prop.getReadMethod() == null ) {
                continue;
            }

            Class<?> type = prop.getPropertyType();
            if ( Map.class.isAssignableFrom(type) ) {
                processMapData(schema, data, mapData, name);
            } else {
                Object newValue = PropertyUtils.getProperty(data, name);
                if ( mapData.containsKey(name) ) {
                    PropertyUtils.setProperty(schema, name, newValue);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void processMapData(SchemaImpl schema, SchemaOverlayImpl data, Map<String, Object> mapData, String name) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String,Object> oldValues = (Map<String, Object>)PropertyUtils.getProperty(schema, name);
        Map<String,Object> newValues = (Map<String, Object>)PropertyUtils.getProperty(data, name);

        Object value = null;
        try {
            value = PropertyUtils.getProperty(data, name + "Explicit");
        } catch ( NoSuchMethodException e ) {
            return;
        }

        if ( Boolean.TRUE.equals(value) ) {
            for ( String key : oldValues.keySet() ) {
                if ( newValues == null || ! newValues.containsKey(key) ) {
                    oldValues.remove(key);
                }
            }
        }

        if ( newValues == null || newValues.size() == 0 ) {
            return;
        }

        for ( String key : newValues.keySet() ) {
            if ( key.startsWith(REMOVE) ) {
                oldValues.remove(StringUtils.removeStart(key, REMOVE));
                continue;
            }

            Object oldValue = oldValues.get(key);
            Object newValue = newValues.get(key);
            if ( newValue == null ) {
                continue;
            }

            if ( oldValue == null ) {
                oldValues.put(key, newValue);
                continue;
            }

            Map<String,Object> mapProperty = (Map<String, Object>)mapData.get(name);
            PropertyUtils.copyProperties(oldValue, mapProperty.get(key));
        }
    }

    protected InputStream loadJsonFile(String prefix, Schema schema) {
        String path = String.format("%s/%s.json", prefix, schema.getId());
        InputStream is = schema.getClass().getClassLoader().getResourceAsStream(path);
        if ( is != null ) {
            log.info("Loading JSON schema overlay for type [{}] from [{}]", schema.getId(), path);
        }
        return is;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public boolean isExplicitByDefault() {
        return explicitByDefault;
    }

    public void setExplicitByDefault(boolean explicitByDefault) {
        this.explicitByDefault = explicitByDefault;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOverridePath() {
        return overridePath;
    }

    public void setOverridePath(String overridePath) {
        this.overridePath = overridePath;
    }

    public io.github.ibuildthecloud.gdapi.json.JsonMapper getSchemaMashaller() {
        return schemaMashaller;
    }

    @Inject
    public void setSchemaMashaller(io.github.ibuildthecloud.gdapi.json.JsonMapper schemaMashaller) {
        this.schemaMashaller = schemaMashaller;
    }

}
