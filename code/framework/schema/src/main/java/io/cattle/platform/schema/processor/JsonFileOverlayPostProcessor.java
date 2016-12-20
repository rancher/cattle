package io.cattle.platform.schema.processor;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.resource.ResourceLoader;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonFileOverlayPostProcessor extends AbstractSchemaPostProcessor implements SchemaPostProcessor, Priority {

    private static final Logger log = LoggerFactory.getLogger(JsonFileOverlayPostProcessor.class);

    public static final String REMOVE = "-";

    JsonMapper jsonMapper;
    io.github.ibuildthecloud.gdapi.json.JsonMapper schemaMashaller;
    boolean explicitByDefault = false;
    boolean whiteList = false;
    Set<String> ignoreTypes = new HashSet<String>();
    Map<String, List<URL>> resources = new HashMap<String, List<URL>>();

    String path;
    ResourceLoader resourceLoader;

    public JsonFileOverlayPostProcessor() {
        ignoreTypes.add("schema");
        ignoreTypes.add("error");
    }

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
        if (ignoreTypes.contains(schema.getId())) {
            return schema;
        }

        try {
            List<URL> resources = lookUpResource(schema.getId());
            if (whiteList && resources.size() == 0) {
                return null;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to lookup schema for [" + schema.getId() + "] at [" + path + "]");
        }

        return super.postProcessRegister(schema, factory);
    }

    protected List<URL> lookUpResource(String id) throws IOException {
        List<URL> result = new ArrayList<URL>();
        String base = String.format("%s/%s.json", path, id);
        String override = String.format("%s/%s.json.d/**/*.json", path, id);

        URL url = getClass().getClassLoader().getResource(base);
        if (url != null) {
            log.info("Loading JSON schema overlay for type [{}] from [{}]", id, url);
            result.add(url);
        }

        for (URL overrideUrl : resourceLoader.getResources(override)) {
            log.info("Loading JSON schema overlay for type [{}] from [{}]", id, overrideUrl);
            result.add(overrideUrl);
        }

        resources.put(id, result);

        return result;
    }

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        if (ignoreTypes.contains(schema.getId())) {
            return schema;
        }

        List<URL> resources = this.resources.get(schema.getId());
        if (resources == null || resources.size() == 0) {
            return schema;
        }

        for (URL resource : resources) {
            InputStream is = null;
            try {
                is = resource.openStream();

                if (is == null) {
                    continue;
                }

                byte[] bytes = IOUtils.toByteArray(is);

                Map<String, Object> mapData = jsonMapper.readValue(bytes);
                SchemaOverlayImpl data = null;
                if (explicitByDefault) {
                    data = schemaMashaller.readValue(bytes, ExplicitByDefaultSchemaOverlayImpl.class);
                } else {
                    data = schemaMashaller.readValue(bytes, SchemaOverlayImpl.class);
                }

                processSchema(schema, data, mapData);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e) {
                throw new IllegalStateException("Error processing " + resource, e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        return schema;
    }

    protected void processSchema(SchemaImpl schema, SchemaOverlayImpl data, Map<String, Object> mapData) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {

        for (PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(schema)) {
            String name = prop.getName();
            Method writeMethod = prop.getWriteMethod();
            if (writeMethod == null || prop.getReadMethod() == null) {
                continue;
            }

            Class<?> type = prop.getPropertyType();
            if (Map.class.isAssignableFrom(type)) {
                processMapData(schema, data, mapData, name);
            } else {
                Object newValue = PropertyUtils.getProperty(data, name);
                if (mapData.containsKey(name)) {
                    PropertyUtils.setProperty(schema, name, newValue);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void processMapData(SchemaImpl schema, SchemaOverlayImpl data, Map<String, Object> mapData, String name) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Map<String, Object> oldValues = (Map<String, Object>) PropertyUtils.getProperty(schema, name);
        Map<String, Object> newValues = (Map<String, Object>) PropertyUtils.getProperty(data, name);

        Object value = null;
        try {
            value = PropertyUtils.getProperty(data, name + "Explicit");
        } catch (NoSuchMethodException e) {
            return;
        }

        if (Boolean.TRUE.equals(value)) {
            for (String key : new HashSet<String>(oldValues.keySet())) {
                if (newValues == null || !newValues.containsKey(key)) {
                    oldValues.remove(key);
                }
            }
        }

        if (newValues == null || newValues.size() == 0) {
            return;
        }

        for (String key : newValues.keySet()) {
            if (key.startsWith(REMOVE)) {
                oldValues.remove(StringUtils.removeStart(key, REMOVE));
                continue;
            }

            Object oldValue = oldValues.get(key);
            Object newValue = newValues.get(key);
            if (newValue == null) {
                continue;
            }

            Map<String, Object> mapProperty = (Map<String, Object>) mapData.get(name);

            if (oldValue == null) {
                BeanUtils.copyProperties(newValue, mapProperty.get(key));
                oldValues.put(key, newValue);
                continue;
            }

            BeanUtils.copyProperties(oldValue, mapProperty.get(key));
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
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

    public io.github.ibuildthecloud.gdapi.json.JsonMapper getSchemaMashaller() {
        return schemaMashaller;
    }

    @Inject
    public void setSchemaMashaller(io.github.ibuildthecloud.gdapi.json.JsonMapper schemaMashaller) {
        this.schemaMashaller = schemaMashaller;
    }

    public boolean isWhiteList() {
        return whiteList;
    }

    public void setWhiteList(boolean whiteList) {
        this.whiteList = whiteList;
    }

    public Set<String> getIgnoreTypes() {
        return ignoreTypes;
    }

    public void setIgnoreTypes(Set<String> ignoreTypes) {
        this.ignoreTypes = ignoreTypes;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Inject
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

}
