package io.cattle.platform.api.auth;

import java.util.List;

public interface Policy {

    public static final String AGENT_ID = "agentId";
    public static final String AUTHORIZED_FOR_ALL_ACCOUNTS = "all.accounts";
    public static final String REMOVED_VISIBLE = "removed.visible";
    public static final String PLAIN_ID = "plain.id";
    public static final String PLAIN_ID_OPTION = "plain.id.option";
    public static final long NO_ACCOUNT = -1L;

    boolean isOption(String optionName);

    String getOption(String optionName);

    List<Long> getAuthorizedAccounts();

    long getAccountId();

    String getUserName();

    <T> List<T> authorizeList(List<T> list);

    <T> T authorizeObject(T obj);

}
