package io.cattle.platform.iaas.api.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.BodyParserRequestHandler;

import org.apache.commons.lang.StringUtils;

public class ExtendedBodyParserRequestHandler extends BodyParserRequestHandler {
    
    @Override
    protected boolean shouldBeParsed(ApiRequest request) {
        return !StringUtils.equalsIgnoreCase("proxy", request.getRequestVersion());
    }

}
