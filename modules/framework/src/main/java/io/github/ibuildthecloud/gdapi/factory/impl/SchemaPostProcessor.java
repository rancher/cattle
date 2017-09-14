package io.github.ibuildthecloud.gdapi.factory.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

public interface SchemaPostProcessor {

    SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory);

    SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory);

}
