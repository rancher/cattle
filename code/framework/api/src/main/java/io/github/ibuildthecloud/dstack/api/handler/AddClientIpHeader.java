package io.github.ibuildthecloud.dstack.api.handler;

import java.io.IOException;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

public class AddClientIpHeader extends AbstractApiRequestHandler {

    public static final String CLIENT_IP = "X-API-Client-IP";

    @Override
    public void handle(ApiRequest request) throws IOException {
        if ( request.isCommitted() ) {
            return;
        }

        request.getServletContext().getResponse().addHeader(CLIENT_IP, request.getClientIp());
    }

}
