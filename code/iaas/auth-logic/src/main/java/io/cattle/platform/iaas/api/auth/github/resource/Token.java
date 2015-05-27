package io.cattle.platform.iaas.api.auth.github.resource;

import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = GithubConstants.TOKEN)
public class Token {

    private final String jwt;
    private final String hostname
            ;
    private String code;
    private final String user;
    private final List<String> orgs;
    private final List<TeamAccountInfo> teams;
    private final Boolean security;
    private final String clientId;
    private final String userType;
    private final String accountId;

    public Token(String jwt, String username, List<String> orgs, List<TeamAccountInfo> teams, Boolean security, String clientId, String userType,
                 String accountId) {
        this.jwt = jwt;
        this.user = username;
        this.hostname = null;
        this.orgs = orgs;
        this.teams = teams;
        this.security = security;
        this.clientId = clientId;
        this.userType = userType;
        this.accountId = accountId;
    }

    public Token(Boolean security, String clientId, String hostName) {
        this.jwt = null;
        this.user = null;
        this.orgs = null;
        this.teams = null;
        this.security = security;
        this.clientId = clientId;
        this.userType = null;
        this.accountId = null;
        this.hostname = hostName;
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

    @Field(nullable = true)
    public String getUserType() {
        return userType;
    }

    @Field(nullable = true)
    public String getAccountId() {
        return accountId;
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }
}
