package io.github.ibuildthecloud.gdapi.exception;

import io.github.ibuildthecloud.gdapi.model.impl.ValidationErrorImpl;

public class ValidationErrorException extends ClientVisibleException {

    private static final long serialVersionUID = 1995702342554398285L;

    public ValidationErrorException(String code, String fieldName) {
        super(new ValidationErrorImpl(code, fieldName));
    }

    public ValidationErrorException(String code, String fieldName, String message) {
        super(new ValidationErrorImpl(code, fieldName));
        ((ValidationErrorImpl)getApiError()).setMessage(message);
    }

}
