package io.cattle.platform.host.model;

import java.util.HashMap;
import java.util.Map;

public class HostApiAccess {

    String url;
    String authenticationToken;
    int port;

    Map<String, String> headers = new HashMap<>();

    protected HostApiAccess() {
    }

    public HostApiAccess(String url, String token, Map<String, String> headers) {
        super();
        this.url = url;
        this.headers = headers;
        this.authenticationToken = token;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

}