package io.cattle.platform.object.defaults;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectDefaultsProvider;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDefaultsProvider implements ObjectDefaultsProvider {

    private static final Logger log = LoggerFactory.getLogger(JsonDefaultsProvider.class);

    SchemaFactory schemaFactory;
    JsonMapper jsonMapper;
    String defaultPath;
    String defaultOverridePath;
    Map<Class<?>, Map<String, Object>> defaults = new HashMap<>();

    public JsonDefaultsProvider(SchemaFactory schemaFactory, JsonMapper jsonMapper, String defaultPath, String defaultOverridePath) {
        super();
        this.schemaFactory = schemaFactory;
        this.jsonMapper = jsonMapper;
        this.defaultPath = defaultPath;
        this.defaultOverridePath = defaultOverridePath;
    }

    @Override
    public Map<? extends Class<?>, ? extends Map<String, Object>> getDefaults() {
        return defaults;
    }

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

}
