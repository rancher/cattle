package io.github.ibuildthecloud.dstack.schema.processor;

import org.apache.commons.lang3.StringUtils;

import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

public class StripSuffixPostProcessor extends AbstractSchemaPostProcessor implements SchemaPostProcessor {

    String suffix;

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactoryImpl factory) {
        if ( ! schema.getId().endsWith(suffix) ) {
            return schema;
        }

        String newName = StringUtils.substringBeforeLast(schema.getId(), suffix);
        schema.setName(newName);

        return schema;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

}