package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

public class NotFoundHandler extends AbstractResponseGenerator {

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (request.getResponseCode() == ResponseCodes.OK)
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
    }

}
