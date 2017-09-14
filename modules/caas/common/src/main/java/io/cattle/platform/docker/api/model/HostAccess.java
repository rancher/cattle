package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list=false)
public class HostAccess {

    String url;
    String token;

    public HostAccess() {
    }

    public HostAccess(String url, String token) {
        super();
        this.url = url;
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
