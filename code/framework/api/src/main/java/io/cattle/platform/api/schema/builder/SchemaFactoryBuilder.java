package io.cattle.platform.api.schema.builder;

import io.cattle.platform.api.schema.ObjectBasedSubSchemaFactory;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.schema.processor.AuthOverlayPostProcessor;
import io.cattle.platform.schema.processor.JsonFileOverlayPostProcessor;
import io.cattle.platform.schema.processor.NotWritablePostProcessor;
import io.cattle.platform.util.resource.ResourceLoader;
import io.cattle.platform.util.resource.URLUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.io.IOException;

public class SchemaFactoryBuilder {

    ObjectBasedSubSchemaFactory factory;

    private SchemaFactoryBuilder() {
    }

    public static SchemaFactoryBuilder id(String id) {
        SchemaFactoryBuilder builder = new SchemaFactoryBuilder();
        builder.factory = new ObjectBasedSubSchemaFactory();
        builder.factory.setId(id);
        return builder;
    }

    public SchemaFactoryBuilder parent(SchemaFactory schemaFactory) {
        factory.setSchemaFactory(schemaFactory);
        return this;
    }

    public SchemaFactoryBuilder jsonAuthOverlay(JsonMapper jsonMapper, String... paths) throws IOException {
        factory.getPostProcessors().add(new AuthOverlayPostProcessor(URLUtils.mustFind(paths), jsonMapper));
        return this;
    }

    public SchemaFactory build() {
        return factory;
    }

    public SchemaFactoryBuilder jsonSchemaFromPath(JsonMapper jsonMapper, io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader, String path) {
        JsonFileOverlayPostProcessor pp = new JsonFileOverlayPostProcessor(resourceLoader, jsonMapper, schemasMarshaller);
        pp.setPath(path);
        factory.getPostProcessors().add(pp);
        return this;
    }

    public SchemaFactoryBuilder whitelistJsonSchemaFromPath(JsonMapper jsonMapper, io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader, String path) {
        JsonFileOverlayPostProcessor pp = new JsonFileOverlayPostProcessor(resourceLoader, jsonMapper, schemasMarshaller);
        pp.setPath(path);
        pp.setWhiteList(true);
        pp.setExplicitByDefault(true);
        factory.getPostProcessors().add(pp);
        return this;
    }

    public SchemaFactoryBuilder notWriteable() {
        NotWritablePostProcessor pp = new NotWritablePostProcessor();
        factory.getPostProcessors().add(pp);
        return this;
    }
}
