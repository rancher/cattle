package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.servlet.ServletException;

public abstract class AbstractApiRequestHandler implements ApiRequestHandler {

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        return false;
    }

}
