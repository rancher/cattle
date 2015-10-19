package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;


@Type(name = TokenUtils.TOKEN)
public class Token {

    private  String jwt;
    private String code;
    private  String user;
    private  Boolean security = SecurityConstants.SECURITY.get();
    private  String userType;
    private  String authProvider = SecurityConstants.AUTH_PROVIDER.get();

    private  String accountId;
    private  Identity userIdentity;
    private  boolean enabled = security;
    private  List<Identity> identities;

    public Token(String jwt, String accountId, Identity userIdentity, List<Identity> identities, String userType) {
        this.jwt = jwt;
        this.userIdentity = userIdentity;
        this.accountId = accountId;
        this.identities = identities;
        this.user = userIdentity.getLogin();
        this.userType = userType;
    }

    public Token() {
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
    public Boolean getSecurity() {
        return security;
    }

    @Field(nullable = true)
    public String getClientId() {
        return SecurityConstants.AUTH_PROVIDER.get().equalsIgnoreCase(GithubConstants.CONFIG)
                ? GithubConstants.GITHUB_CLIENT_ID.get() : null;
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
        return SecurityConstants.AUTH_PROVIDER.get().equalsIgnoreCase(GithubConstants.CONFIG)
                ? GithubConstants.GITHUB_HOSTNAME.get() : null;
    }


    @Field(nullable = true, required = true)
    public String getCode() {
        return code;
    }

    @Field(nullable = true)
    public Identity getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(Identity user) {
        this.userIdentity = user;
    }

    @Field(nullable = true)
    public boolean isEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public Identity[] getIdentities() {
        return identities.toArray(new Identity[identities.size()]);
    }

    public void setIdentities(List<Identity> identities) {
        this.identities = identities;
    }

    @Field(nullable = true)
    public String getAuthProvider() {
        return authProvider;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
