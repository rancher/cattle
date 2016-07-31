package io.cattle.platform.host.stats.api;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list=false)
public class StatsAccess {

    String url;
    String token;

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
