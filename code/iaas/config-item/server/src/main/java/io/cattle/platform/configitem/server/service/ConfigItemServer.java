package io.cattle.platform.configitem.server.service;

import io.cattle.platform.configitem.server.model.Request;

import java.io.IOException;

public interface ConfigItemServer {

    void handleRequest(Request req) throws IOException;

    void syncSourceVersion();

}
