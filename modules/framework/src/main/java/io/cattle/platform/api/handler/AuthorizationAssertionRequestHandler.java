package io.cattle.platform.api.handler;

import io.cattle.platform.api.utils.ApiUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;

public class AuthorizationAssertionRequestHandler implements ApiRequestHandler {

    @Override
    public void handle(ApiRequest request) throws IOException {
        Object obj = request.getResponseObject();
        if (obj != null && obj.getClass() != Object.class) {
            request.setResponseObject(ApiUtils.authorize(obj));
        }
    }

}
