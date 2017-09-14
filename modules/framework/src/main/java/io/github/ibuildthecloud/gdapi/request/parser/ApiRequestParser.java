package io.github.ibuildthecloud.gdapi.request.parser;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface ApiRequestParser {

    boolean parse(ApiRequest apiRequest) throws IOException;

    String parseVersion(String servletPath);

}
