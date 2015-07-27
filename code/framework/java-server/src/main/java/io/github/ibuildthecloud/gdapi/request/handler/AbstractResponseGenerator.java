package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;

public abstract class AbstractResponseGenerator extends AbstractApiRequestHandler {

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (RequestUtils.hasBeenHandled(request))
            return;
        generate(request);
    }

    protected abstract void generate(ApiRequest request) throws IOException;
}
