package io.cattle.platform.iaas.api.auth.github;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "token")
public class Token {

    private final String jwt;
    private String code;
    private final String user;
    private final List<String> orgs;
    private final Boolean security;
    private final String clientId;

    public Token(String jwt, String username, List<String> orgs, Boolean security, String clientId) {
        this.jwt = jwt;
        this.user = username;
        this.orgs = orgs;
        this.security = security;
        this.clientId = clientId;
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

    public String getUser() {
        return user;
    }

    public List<String> getOrgs() {
        return orgs;
    }
    
    public Boolean getSecurity() {
        return security;
    }
    
    public String getClientId() {
        return clientId;
    }
}
