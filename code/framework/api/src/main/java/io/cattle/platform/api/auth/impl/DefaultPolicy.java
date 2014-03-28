package io.cattle.platform.api.auth.impl;

import io.cattle.platform.api.auth.Policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultPolicy implements Policy {

    long accountId;
    String name;
    List<Long> authorizedAccounts;
    PolicyOptions options;

    @SuppressWarnings("unchecked")
    public DefaultPolicy() {
        this(Policy.NO_ACCOUNT, null, Collections.EMPTY_LIST, new NoPolicyOptions());
    }

    public DefaultPolicy(long accountId, String name, List<Long> authorizedAccounts, PolicyOptions options) {
        super();
        this.accountId = accountId;
        this.authorizedAccounts = authorizedAccounts;
        this.options = options;
        this.name = name;
    }

    @Override
    public List<Long> getAuthorizedAccounts() {
        return authorizedAccounts;
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
        for ( T obj : list ) {
            T authorized = authorizeObject(obj);
            if ( authorized != null )
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

}
