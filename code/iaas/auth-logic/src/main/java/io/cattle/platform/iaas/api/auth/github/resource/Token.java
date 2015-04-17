package io.cattle.platform.iaas.api.auth.github.resource;

import io.cattle.platform.core.constants.ProjectConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

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
    private final String userType;
    private final String defaultProject;
    private final String accountId;

    public Token(String jwt, String username, List<String> orgs, List<TeamAccountInfo> teams, Boolean security, String clientId, String userType,
            String defaultProjectId, String accountId) {
        this.jwt = jwt;
        this.user = username;
        this.orgs = orgs;
        this.teams = teams;
        this.security = security;
        this.clientId = clientId;
        this.userType = userType;
        this.defaultProject = defaultProjectId;
        this.accountId = accountId;
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
    public String getDefaultProject() {
        return defaultProject;
    }

    @Field(nullable = true)
    public String getAccountId() {
        return accountId;
    }
}
