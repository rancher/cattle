package io.github.ibuildthecloud.gdapi.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "error", list = false)
public interface ApiError extends ApiStandardType {

    int getStatus();

    String getCode();

    String getMessage();

    String getDetail();

}
