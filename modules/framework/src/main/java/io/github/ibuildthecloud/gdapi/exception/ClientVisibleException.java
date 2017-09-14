package io.github.ibuildthecloud.gdapi.exception;

import io.github.ibuildthecloud.gdapi.model.ApiError;

public class ClientVisibleException extends RuntimeException {

    private static final long serialVersionUID = 4255804727056829320L;

    ApiError apiError;
    int status;
    String code, detail;

    public ClientVisibleException(ApiError apiError) {
        super();
        this.apiError = apiError;
    }

    public ClientVisibleException(int status, String code, String message, String detail) {
        super(message == null ? code : message);
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

    public ClientVisibleException(int status, String code) {
        this(status, code, null, null);
    }

    public ClientVisibleException(int status) {
        this(status, null);
    }

    public ApiError getApiError() {
        return apiError;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

}
