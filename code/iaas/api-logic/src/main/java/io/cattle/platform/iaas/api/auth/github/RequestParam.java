package io.cattle.platform.iaas.api.auth.github;

import org.apache.http.NameValuePair;

public class RequestParam implements NameValuePair {

    private String name;
    private String value;

    public RequestParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

}
