package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
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
        } else if ( policy.isOption(Policy.PLAIN_ID_OPTION) && "true".equals(request.getOptions().get("_plainId")) ) {
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
