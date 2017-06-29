package io.cattle.platform.util.resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class URLUtils {

    public static URL mustFind(String resource) {
        URL url = URLUtils.class.getResource("/" + resource);
        if (url == null) {
            throw new IllegalArgumentException("Failed to find [" + resource + "]");
        }
        return url;
    }

    public static List<URL> mustFind(String... resources) {
        List<URL> urls = new ArrayList<>(resources.length);
        for (String resource : resources) {
            urls.add(mustFind(resource));
        }
        return urls;
    }

}
