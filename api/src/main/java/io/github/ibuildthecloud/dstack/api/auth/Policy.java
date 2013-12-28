package io.github.ibuildthecloud.dstack.api.auth;

import java.util.List;

public interface Policy {

    public static final String AUTHORIZED_FOR_ALL_ACCOUNTS = "all.accounts";
    public static final String REMOVED_VISIBLE = "removed.visible";
    public static final long NO_ACCOUNT = -1L;

    boolean isOption(String optionName);

    String getOption(String optionName);

    List<Long> getAuthorizedAccounts();

    long getAccountId();

    <T> List<T> authorizeList(List<T> list);

    <T> T authorizeObject(T obj);

}
