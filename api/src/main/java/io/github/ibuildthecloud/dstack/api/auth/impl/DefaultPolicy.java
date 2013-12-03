package io.github.ibuildthecloud.dstack.api.auth.impl;

import io.github.ibuildthecloud.dstack.api.auth.Policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultPolicy implements Policy {

    @Override
    public boolean isAuthorizedForAllAccounts() {
        return false;
    }

    @Override
    public List<Long> getAuthorizedAccounts() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRemovedVisible() {
        return false;
    }

    @Override
    public <T> List<T> authorized(List<T> list) {
        List<T> result = new ArrayList<T>(list.size());
        for ( T obj : list ) {
            T authorized = authorize(obj);
            if ( authorized != null )
                result.add(authorized);
        }
        return list;
    }

    @Override
    public <T> T authorize(T obj) {
        return obj;
    }

}
