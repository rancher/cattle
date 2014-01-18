package io.github.ibuildthecloud.dstack.configitem.server.model;

import io.github.ibuildthecloud.dstack.util.type.Named;

import java.io.IOException;

public interface ConfigItem extends Named {

    void handleRequest(Request req) throws IOException;

    String getSourceRevision();

}
