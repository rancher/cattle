package io.github.ibuildthecloud.dstack.api.auth;

import java.util.List;

public interface Policy {

    boolean isAuthorizedForAllAccounts();

    boolean isRemovedVisible();

    List<Long> getAuthorizedAccounts();

    <T> List<T> authorized(List<T> list);

    <T> T authorize(T obj);

}
