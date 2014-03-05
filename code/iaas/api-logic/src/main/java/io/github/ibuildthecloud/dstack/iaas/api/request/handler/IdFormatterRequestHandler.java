package io.github.ibuildthecloud.dstack.iaas.api.request.handler;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;

import javax.inject.Inject;

public class IdFormatterRequestHandler extends AbstractApiRequestHandler implements ApiRequestHandler {

    IdentityFormatter plainFormatter;

    @Override
    public void handle(ApiRequest request) throws IOException {
        Policy policy = ApiUtils.getPolicy();
        if ( policy.isOption(Policy.PLAIN_ID) ) {
            ApiContext.getContext().setIdFormatter(plainFormatter);
        }
    }

    public IdentityFormatter getPlainFormatter() {
        return plainFormatter;
    }

    @Inject
    public void setPlainFormatter(IdentityFormatter plainFormatter) {
        this.plainFormatter = plainFormatter;
    }

}
