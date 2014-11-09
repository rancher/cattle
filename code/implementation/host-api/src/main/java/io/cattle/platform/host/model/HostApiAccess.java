package io.cattle.platform.host.model;

import java.util.HashMap;
import java.util.Map;

public class HostApiAccess {

    String hostname;
    String authenticationToken;

    Map<String, String> headers = new HashMap<>();

    protected HostApiAccess() {
    }

    public HostApiAccess(String hostname, String token, Map<String, String> headers) {
        super();
        this.hostname = hostname;
        this.headers = headers;
        this.authenticationToken = token;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHostname() {
        return hostname;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

}