package io.cattle.platform.schema.processor;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import org.apache.commons.lang3.StringUtils;

public class StripSuffixPostProcessor extends AbstractSchemaPostProcessor implements SchemaPostProcessor {

    String suffix;

    public StripSuffixPostProcessor(String suffix) {
        super();
        this.suffix = suffix;
    }

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
        if (!schema.getId().endsWith(suffix)) {
            return schema;
        }

        String newName = StringUtils.substringBeforeLast(schema.getId(), suffix);
        schema.setName(newName);

        return schema;
    }

    public String getSuffix() {
        return suffix;
    }

}