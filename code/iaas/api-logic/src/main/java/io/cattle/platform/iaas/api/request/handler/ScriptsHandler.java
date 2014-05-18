package io.cattle.platform.iaas.api.request.handler;

import java.io.IOException;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ScriptsHandler {

    boolean handle(ApiRequest request) throws IOException;

}
