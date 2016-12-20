package io.cattle.platform.app;

import io.cattle.platform.schema.processor.AuthSchemaAdditionsPostProcessor;
import io.cattle.platform.schema.processor.JpaWritablePostProcessor;
import io.cattle.platform.schema.processor.JsonFileOverlayPostProcessor;
import io.cattle.platform.schema.processor.StripSuffixPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TypesConfig {

    @Bean
    SchemaFactoryImpl CoreSchemaFactory() {
        return new SchemaFactoryImpl();
    }

    @Bean
    JsonFileOverlayPostProcessor BaseSchemaOverlay() {
        JsonFileOverlayPostProcessor postProcessor = new JsonFileOverlayPostProcessor();
        postProcessor.setPath("schema/base");
        return postProcessor;
    }

    @Bean
    JpaWritablePostProcessor jpaWritablePostProcessor() {
        JpaWritablePostProcessor postProcess = new JpaWritablePostProcessor();
        return postProcess;
    }

    @Bean
    StripSuffixPostProcessor stripSuffixPostProcessor() {
        StripSuffixPostProcessor postProcessor = new StripSuffixPostProcessor();
        postProcessor.setSuffix("Record");
        return postProcessor;
    }

    @Bean
    AuthSchemaAdditionsPostProcessor authSchemaAdditionsPostProcessor() {
        AuthSchemaAdditionsPostProcessor postProcessor = new AuthSchemaAdditionsPostProcessor();
        return postProcessor;
    }
}
