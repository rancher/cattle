package io.cattle.platform.configitem.server.model;

import io.cattle.platform.util.type.Named;

import java.io.IOException;

public interface ConfigItem extends Named {

    void handleRequest(Request req) throws IOException;

    String getSourceRevision();

    boolean isDynamicallyApplied();

}
