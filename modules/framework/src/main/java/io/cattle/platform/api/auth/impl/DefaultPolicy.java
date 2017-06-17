package io.cattle.platform.api.auth.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DefaultPolicy implements Policy {

    long accountId;
    Long clusterId;
    long authenticatedAsAccountId;
    String name;
    Set<Identity> identities;
    PolicyOptions options;

    public DefaultPolicy() {
        this(NO_ACCOUNT, NO_ACCOUNT, NO_ACCOUNT, null, Collections.emptySet(), new NoPolicyOptions());
    }

    public DefaultPolicy(long accountId, long authenticatedAsAccountId, Long clusterId, String name, Set<Identity> identities, PolicyOptions options) {
        super();
        this.accountId = accountId;
        this.clusterId = clusterId;
        this.authenticatedAsAccountId = authenticatedAsAccountId;
        this.identities = identities;
        this.options = options;
        this.name = name;
    }

    @Override
    public Set<Identity> getIdentities(){
        return identities;
    }

    @Override
    public boolean isOption(String optionName) {
        return options.isOption(optionName);
    }

    @Override
    public String getOption(String optionName) {
        return options.getOption(optionName);
    }

    @Override
    public void setOption(String optionName, String value) {
        options.setOption(optionName, value);
    }

    @Override
    public <T> T checkAuthorized(T obj) {
        return obj;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Long getClusterId() {
        return clusterId;
    }

    @Override
    public long getAuthenticatedAsAccountId() {
        return authenticatedAsAccountId;
    }

    @Override
    public String getUserName() {
        return name;
    }

    @Override
    public <T> void grantObjectAccess(T obj) {
        ApiRequest apiRequest = ApiContext.getContext().getApiRequest();
        @SuppressWarnings("unchecked")
        Set<Object> whitelist = (Set<Object>) (apiRequest.getAttribute("whitelist"));
        if (whitelist == null) {
            whitelist = new HashSet<>();
            apiRequest.setAttribute("whitelist", whitelist);
        }
        whitelist.add(obj);
    }

    protected <T> boolean hasGrantedAccess(T obj) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        @SuppressWarnings("unchecked")
        Set<Object> whitelist = (Set<Object>) request.getAttribute("whitelist");
        return (null != whitelist && whitelist.contains(obj));
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }

}
