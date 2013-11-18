package io.github.ibuildthecloud.dstack.object.defaults;

import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectDefaultsProvider;
import io.github.ibuildthecloud.dstack.util.init.AfterExtensionInitialization;
import io.github.ibuildthecloud.dstack.util.init.InitializationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDefaultsProvider implements ObjectDefaultsProvider {

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

    @PostConstruct
    public void init() {
        InitializationUtils.onInitialization(this, schemaFactory);
    }

    @AfterExtensionInitialization
    protected void loadDefaults() throws IOException {
        for ( Schema schema : schemaFactory.listSchemas() ) {
            Class<?> clz = schemaFactory.getSchemaClass(schema.getId());
            if ( clz == null )
                continue;

            InputStream is = null;
            try {
                is = jsonFile(defaultOverridePath, schema);
                if ( is == null ) {
                    is = jsonFile(defaultPath, schema);
                }

                if ( is != null ) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> defaults = jsonMapper.readValue(is, Map.class);
                    this.defaults.put(clz, defaults);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    protected InputStream jsonFile(String prefix, Schema schema) {
        String path = String.format("%s/%s.json", prefix, schema.getId());
        InputStream is = schema.getClass().getClassLoader().getResourceAsStream(path);
        if ( is != null ) {
            log.info("Loading defaults for type [{}] from [{}]", schema.getId(), path);
        }
        return is;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    @Inject
    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public String getDefaultOverridePath() {
        return defaultOverridePath;
    }

    @Inject
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
