package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.integration.github.resource.TeamAccountInfo;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;


@Type(name = TokenUtils.TOKEN)
public class Token {

    private  String jwt;
    private  String hostname;
    private String code;
    private  String user;
    private  List<String> orgs;
    private  List<TeamAccountInfo> teams;
    private  Boolean security;
    private  String clientId;
    private  String userType;
    private  String authProvider;

    private  String accountId;
    private  Identity userIdentity;
    private  boolean enabled;
    private  List<Identity> identities;

    //LEGACY: Used for old Implementation of projects/ Identities. Remove when vincent changes to new api.
    public Token(String jwt, String username, List<String> orgs, List<TeamAccountInfo> teams, Boolean security, String clientId, String userType,
                 String authProvider, String accountId, List<Identity> identities, Identity userIdentity) {
        this.jwt = jwt;
        this.user = username;
        this.authProvider = authProvider;
        this.orgs = orgs;
        this.teams = teams;
        this.security = security;
        this.clientId = clientId;
        this.userType = userType;
        this.accountId = accountId;
        this.userIdentity = userIdentity;
        this.enabled = this.security;
        this.identities = identities;

    }

    //LEGACY: Used for old Implementation of projects/ Identities. Remove when vincent changes to new api.
    public Token(Boolean security, String clientId, String hostName, String authProvider) {
        this.authProvider = authProvider;
        this.security = security;
        this.clientId = clientId;
        this.hostname = hostName;
        enabled = this.security;
    }

    public Token(String jwt, String authProvider, String accountId, Identity userIdentity, List<Identity> identities, boolean enabled, String userType) {
        this.jwt = jwt;
        this.authProvider = authProvider;
        this.userIdentity = userIdentity;
        this.accountId = accountId;
        this.identities = identities;
        this.enabled = enabled;
        this.user = userIdentity.getName();
        this.userType = userType;
    }

    public Token(String authProvider, boolean enabled) {
        this.authProvider = authProvider;
        this.enabled = enabled;
    }

    @Field(nullable = true)
    public String getJwt() {
        return jwt;
    }

    @Field(nullable = true)
    public void setCode(String code) {
        this.code = code;
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


    @Field(nullable = true, required = true)
    public String getCode() {
        return code;
    }

    @Field(nullable = true)
    public Identity getUserIdentity() {
        return userIdentity;
    }

    @Field(nullable = true)
    public boolean isEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public Identity[] getIdentities() {
        return (Identity[]) identities.toArray();
    }

    @Field(nullable = true)
    public String getAuthProvider() {
        return authProvider;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
