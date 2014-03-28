package io.cattle.platform.configitem.server.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Resource {
    String getName();

    URL getURL();

    long getSize();

    InputStream getInputStream() throws IOException;

    void setAttribute(String key, Object value);

    Object getAttibute(String key);
}
