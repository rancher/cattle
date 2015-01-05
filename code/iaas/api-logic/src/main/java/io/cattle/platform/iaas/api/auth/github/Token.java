package io.cattle.platform.iaas.api.auth.github;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "token")
public class Token {

    private final String jwt;

    public Token(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
}
