package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public abstract class AbstractApiRequestHandler implements ApiRequestHandler {

    @Override
    public boolean handleException(ApiRequest request, Throwable e) {
        return false;
    }

}
