package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class NoOpApiRequestHandler extends AbstractApiRequestHandler implements ApiRequestHandler {

    @Override
    public void handle(ApiRequest request) {
    }

}
