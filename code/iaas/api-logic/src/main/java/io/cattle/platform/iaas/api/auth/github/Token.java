package io.cattle.platform.iaas.api.auth.github;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "token")
public class Token {

    private final String jwt;

    private String code;

    public Token(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Field(required = true)
    public String getCode() {
        return code;
    }
}
