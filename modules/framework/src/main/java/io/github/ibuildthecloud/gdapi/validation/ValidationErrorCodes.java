package io.github.ibuildthecloud.gdapi.validation;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.impl.ValidationErrorImpl;

public class ValidationErrorCodes implements ErrorCodes {

    public static final String UNSUPPORTED_VERSION = "UnsupportedVersion";
    public static final String INVALID_DATE_FORMAT = "InvalidDateFormat";
    public static final String INVALID_FORMAT = "InvalidFormat";
    public static final String INVALID_REFERENCE = "InvalidReference";

    public static final String NOT_NULLABLE = "NotNullable";
    public static final String NOT_UNIQUE = "NotUnique";

    public static final String MIN_LIMIT_EXCEEDED = "MinLimitExceeded";
    public static final String MAX_LIMIT_EXCEEDED = "MaxLimitExceeded";

    public static final String MIN_LENGTH_EXCEEDED = "MinLengthExceeded";
    public static final String MAX_LENGTH_EXCEEDED = "MaxLengthExceeded";

    public static final String INVALID_OPTION = "InvalidOption";

    public static final String INVALID_CHARACTERS = "InvalidCharacters";

    public static final String MISSING_REQUIRED = "MissingRequired";

    public static final String INVALID_CSRF_TOKEN = "InvalidCSRFToken";

    public static final String INVALID_ACTION = "InvalidAction";

    public static final String INVALID_BODY_CONTENT = "InvalidBodyContent";

    public static final String ACTION_NOT_AVAILABLE = "ActionNotAvailable";

    public static final String INVALID_STATE = "InvalidState";

    public static final void throwValidationError(String code, String fieldName) {
        throw new ClientVisibleException(new ValidationErrorImpl(code, fieldName));
    }
}
