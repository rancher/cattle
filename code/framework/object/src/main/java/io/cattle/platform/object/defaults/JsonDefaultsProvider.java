package io.cattle.platform.object.defaults;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectDefaultsProvider;
import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDefaultsProvider implements ObjectDefaultsProvider, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(JsonDefaultsProvider.class);

    SchemaFactory schemaFactory;
    JsonMapper jsonMapper;
    String defaultPath;
    String defaultOverridePath;
    Map<Class<?>, Map<String, Object>> defaults = new HashMap<Class<?>, Map<String, Object>>();

    @Override
    public Map<? extends Class<?>, ? extends Map<String, Object>> getDefaults() {
        return defaults;
    }

    @Override
    public void start() {
        for (Schema schema : schemaFactory.listSchemas()) {
            Class<?> clz = schemaFactory.getSchemaClass(schema.getId());
            if (clz == null)
                continue;

            InputStream is = null;
            try {
                is = jsonFile(defaultOverridePath, schema);
                if (is == null) {
                    is = jsonFile(defaultPath, schema);
                }

                if (is != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> defaults = jsonMapper.readValue(is, Map.class);
                    Map<String, Object> existing = this.defaults.get(clz);
                    if (existing != null) {
                        existing.putAll(defaults);
                        defaults = existing;
                    }
                    this.defaults.put(clz, defaults);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    protected InputStream jsonFile(String prefix, Schema schema) {
        String path = String.format("%s/%s.json", prefix, schema.getId());
        InputStream is = schema.getClass().getClassLoader().getResourceAsStream(path);
        if (is != null) {
            log.info("Loading defaults for type [{}] from [{}]", schema.getId(), path);
        }
        return is;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public String getDefaultOverridePath() {
        return defaultOverridePath;
    }

    public void setDefaultOverridePath(String defaultOverridePath) {
        this.defaultOverridePath = defaultOverridePath;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
