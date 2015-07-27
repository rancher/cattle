package io.github.ibuildthecloud.gdapi.model;

public interface ValidationError extends ApiError {

    String getFieldName();

}
