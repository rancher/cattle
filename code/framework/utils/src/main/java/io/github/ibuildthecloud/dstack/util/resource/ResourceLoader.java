package io.github.ibuildthecloud.dstack.util.resource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface ResourceLoader {

    List<URL> getResources(String path) throws IOException;

}
