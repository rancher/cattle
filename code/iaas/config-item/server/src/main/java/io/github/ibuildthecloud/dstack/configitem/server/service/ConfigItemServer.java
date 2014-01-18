package io.github.ibuildthecloud.dstack.configitem.server.service;

import io.github.ibuildthecloud.dstack.configitem.server.model.Request;

import java.io.IOException;

public interface ConfigItemServer {

    void handleRequest(Request req) throws IOException;

}
