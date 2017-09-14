package io.cattle.platform.api.requesthandler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface ScriptsHandler {

    boolean handle(ApiRequest request) throws IOException;

}
