package io.cattle.platform.api.auth;

import java.util.List;
import java.util.Set;

public interface Policy {

    public static final String AGENT_ID = "agentId";
    public static final String LIST_ALL_ACCOUNTS = "list.all.accounts";
    public static final String AUTHORIZED_FOR_ALL_ACCOUNTS = "all.accounts";
    public static final String LIST_ALL_SETTINGS = "list.all.settings";
    public static final String REMOVED_VISIBLE = "removed.visible";
    public static final String PLAIN_ID = "plain.id";
    public static final String PLAIN_ID_OPTION = "plain.id.option";
    public static final String ROLE_OPTION = "role.option";
    public static final long NO_ACCOUNT = -1L;

    boolean isOption(String optionName);

    String getOption(String optionName);

    Set<Identity> getIdentities();

    long getAccountId();

    long getAuthenticatedAsAccountId();

    String getUserName();

    <T> List<T> authorizeList(List<T> list);

    <T> T authorizeObject(T obj);

    <T> void grantObjectAccess(T obj);

}
