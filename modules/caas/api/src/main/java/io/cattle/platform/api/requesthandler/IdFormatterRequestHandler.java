package io.cattle.platform.api.requesthandler;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;

public class IdFormatterRequestHandler implements ApiRequestHandler {

    IdentityFormatter plainFormatter = new IdentityFormatter();

    @Override
    public void handle(ApiRequest request) throws IOException {
        Policy policy = ApiUtils.getPolicy();
        if (policy.isOption(Policy.PLAIN_ID)) {
            ApiContext.getContext().setIdFormatter(plainFormatter);
        } else if (policy.isOption(Policy.PLAIN_ID_OPTION) && "true".equalsIgnoreCase(request.getOptions().get("_plainId"))) {
            ApiContext.getContext().setIdFormatter(plainFormatter);
        }
        ApiContext.getContext().setIdFormatter(ApiContext.getContext().getIdFormatter().withSchemaFactory(request.getSchemaFactory()));
    }

}
