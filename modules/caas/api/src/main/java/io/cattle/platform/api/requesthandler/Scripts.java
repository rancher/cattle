package io.cattle.platform.api.requesthandler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.List;

public class Scripts extends AbstractResponseGenerator {

    private static final String SCRIPTS = "scripts";

    List<ScriptsHandler> handlers;

    public Scripts(List<ScriptsHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (!SCRIPTS.equals(request.getType())) {
            return;
        }

        request.setResponseContentType("text/plain");
        boolean handled = false;

        for (ScriptsHandler handler : handlers) {
            handled = handler.handle(request);
            if (handled) {
                break;
            }
        }

        if (!handled) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }

        if ( request.getResponseObject() == null ) {
            request.setResponseObject(new Object());
            request.commit();
        }
    }

    public List<ScriptsHandler> getHandlers() {
        return handlers;
    }

}
