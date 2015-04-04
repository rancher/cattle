package io.cattle.platform.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultPolicy implements Policy {

    long accountId;
    String name;
    List<ExternalId> externalIds;
    PolicyOptions options;

    @SuppressWarnings("unchecked")
    public DefaultPolicy() {
        this(Policy.NO_ACCOUNT, null, Collections.EMPTY_LIST, new NoPolicyOptions());
    }

    public DefaultPolicy(long accountId, String name, List<ExternalId> externalIds, PolicyOptions options) {
        super();
        this.accountId = accountId;
        this.externalIds = externalIds;
        this.options = options;
        this.name = name;

        if (this.externalIds == null) {
            this.externalIds = Collections.emptyList();
        }
    }

    @Override
    public List<ExternalId> getExternalIds() {
        return this.externalIds;
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
    public <T> List<T> authorizeList(List<T> list) {
        List<T> result = new ArrayList<T>(list.size());
        for (T obj : list) {
            T authorized = authorizeObject(obj);
            if (authorized != null)
                result.add(authorized);
        }
        return result;
    }

    @Override
    public <T> T authorizeObject(T obj) {
        return obj;
    }

    @Override
    public long getAccountId() {
        return accountId;
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
        }
        whitelist.add(obj);
        apiRequest.setAttribute("whitelist", whitelist);
    }

    protected <T> boolean hasGrantedAccess(T obj) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        @SuppressWarnings("unchecked")
        Set<Object> whitelist = (Set<Object>) request.getAttribute("whitelist");
        return (null != whitelist && whitelist.contains(obj));
    }

}
