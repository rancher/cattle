package io.github.ibuildthecloud.gdapi.validation;

import io.github.ibuildthecloud.gdapi.model.Resource;

public interface ReferenceValidator {

    Object getById(String type, String id);

    Resource getResourceId(String type, String id);

    Object getByField(String type, String fieldName, Object value, String id);

}
