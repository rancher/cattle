package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

public class SchemasHandler extends AbstractResponseGenerator {

    @Override
    protected void generate(ApiRequest request) throws IOException {
        SchemaFactory schemaFactory = request.getSchemaFactory();

        if (!schemaFactory.typeStringMatches(Schema.class, request.getType()))
            return;

        if (request.getId() == null) {
            request.setResponseObject(schemaFactory.listSchemas());
        } else {
            Schema lookup = schemaFactory.getSchema(request.getId());
            if (lookup == null) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
            request.setResponseObject(lookup);
        }
    }

}
