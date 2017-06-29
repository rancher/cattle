package io.cattle.platform.util.resource;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClassloaderResourceLoader implements ResourceLoader {

    @Override
    public List<URL> getResources(String path) throws IOException {
        URL url = getClass().getClassLoader().getResource(path);
        return url == null ? Collections.emptyList() : Arrays.asList(url);
    }

}
