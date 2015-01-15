package io.cattle.platform.iaas.api.auth.github.resource;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "token")
public class Token {

    private final String jwt;
    private String code;
    private final String user;
    private final List<String> orgs;
    private final List<TeamAccountInfo> teams;
    private final Boolean security;
    private final String clientId;

    public Token(String jwt, String username, List<String> orgs, List<TeamAccountInfo> teams, Boolean security, String clientId) {
        this.jwt = jwt;
        this.user = username;
        this.orgs = orgs;
        this.teams = teams;
        this.security = security;
        this.clientId = clientId;
    }

    @Field(nullable = true)
    public String getJwt() {
        return jwt;
    }

    @Field(nullable = true)
    public void setCode(String code) {
        this.code = code;
    }

    @Field(required = true, nullable = true)
    public String getCode() {
        return code;
    }

    @Field(nullable = true)
    public String getUser() {
        return user;
    }

    @Field(nullable = true)
    public List<String> getOrgs() {
        return orgs;
    }

    @Field(nullable = true)
    public List<TeamAccountInfo> getTeams() {
        return teams;
    }

    @Field(nullable = true)
    public Boolean getSecurity() {
        return security;
    }

    @Field(nullable = true)
    public String getClientId() {
        return clientId;
    }

}
