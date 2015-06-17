package io.cattle.platform.framework.encryption.request.handler;

import io.github.ibuildthecloud.gdapi.util.TransformationService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TransformationHandler extends AbstractApiRequestHandler {

    @Inject
    TransformationService transformationService;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (request.isCommitted() || request.getRequestObject() == null)
            return;
        Map<String, Object> object = CollectionUtils.toMap(request.getRequestObject());
        if (object.keySet().isEmpty()){
            return;
        }
        Schema schema = request.getSchemaFactory().getSchema(request.getType());
        if (schema == null){
            return;
        }
        Map<String, Field> fields = schema.getResourceFields();
        for (String fieldName: object.keySet()){
            if (!fields.containsKey(fieldName) || object.get(fieldName) == null){
                continue;
            }
            Field field = fields.get(fieldName);
            if (StringUtils.isNotBlank(field.getTransform())){
                String encrypted = transformationService.transform((String) object.get(fieldName), field.getTransform());
                object.put(fieldName, encrypted);
            }
        }
    }
}
