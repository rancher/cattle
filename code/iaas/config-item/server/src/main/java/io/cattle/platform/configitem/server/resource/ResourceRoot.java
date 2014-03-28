package io.cattle.platform.configitem.server.resource;

import java.io.IOException;
import java.util.Collection;

public interface ResourceRoot {

    Collection<Resource> getResources() throws IOException;

    void scan() throws IOException;

    String getSourceRevision();
}
