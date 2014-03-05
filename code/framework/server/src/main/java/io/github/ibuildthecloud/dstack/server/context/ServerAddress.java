package io.github.ibuildthecloud.dstack.server.context;

import java.net.MalformedURLException;
import java.net.URL;

public class ServerAddress {

    String urlString;
    URL url;

    public ServerAddress(String url) {
        this.urlString = url;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL [" + url + "]", e);
        }
    }

    public String getUrlString() {
        return urlString;
    }

    public URL getUrl() {
        return url;
    }

}
