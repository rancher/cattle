package io.cattle.platform.api.handler;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;

public class DeferredActionsHandler extends AbstractApiRequestHandler {

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (request.getExceptions().size() > 0) {
            DeferredUtils.resetDeferred();
        }
    }

}
