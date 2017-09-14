package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.model.ValidationError;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class ValidationErrorImpl extends ErrorImpl implements ValidationError {

    String fieldName;

    public ValidationErrorImpl(String code, String fieldName) {
        super(ResponseCodes.UNPROCESSABLE_ENTITY, code, null, null);
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

}
