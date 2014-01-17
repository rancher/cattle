package io.github.ibuildthecloud.dstack.api.handler;

import java.io.IOException;

import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

public class DeferredActionsHandler extends AbstractApiRequestHandler {

    @Override
    public void handle(ApiRequest request) throws IOException {
        if ( request.getExceptions().size() > 0 ) {
            DeferredUtils.resetDeferred();
        }
    }

}
