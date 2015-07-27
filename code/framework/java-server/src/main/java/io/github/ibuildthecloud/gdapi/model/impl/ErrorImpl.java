package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.ApiError;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class ErrorImpl extends ResourceImpl implements ApiError {

    int status;
    String code, message, detail;

    public ErrorImpl() {
        this.status = ResponseCodes.UNPROCESSABLE_ENTITY;
        setType("error");
        setId(UUID.randomUUID().toString());
    }

    public ErrorImpl(ClientVisibleException e) {
        this(e.getStatus(), e.getCode(), e.getMessage(), e.getDetail());
    }

    public ErrorImpl(int status) {
        this(status, null);
    }

    public ErrorImpl(int status, String code) {
        this(status, code, null, null);
    }

    public ErrorImpl(int status, String code, String message, String detail) {
        this();
        this.status = status;
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    @Override
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public Map<String, URL> getLinks() {
        Map<String, URL> links = super.getLinks();
        links.remove(UrlBuilder.SELF);
        return links;
    }
}
