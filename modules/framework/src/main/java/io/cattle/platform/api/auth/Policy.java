package io.cattle.platform.api.auth;

import java.util.Set;

public interface Policy {

    String AGENT_ID = "agentId";
    String LIST_ALL_ACCOUNTS = "list.all.accounts";
    String AUTHORIZED_FOR_ALL_ACCOUNTS = "all.accounts";
    String RESOURCE_ACCOUNT_ID = "resource.account.id";
    String LIST_ALL_SETTINGS = "list.all.settings";
    String PLAIN_ID = "plain.id";
    String PLAIN_ID_OPTION = "plain.id.option";
    String ROLE_OPTION = "role.option";
    String ASSIGNED_ROLE = "assigned.role";
    long NO_ACCOUNT = -1L;

    boolean isOption(String optionName);

    String getOption(String optionName);

    void setOption(String optionName, String value);

    Set<Identity> getIdentities();

    long getAccountId();

    long getAuthenticatedAsAccountId();

    String getUserName();

    <T> T checkAuthorized(T obj);

    <T> void grantObjectAccess(T obj);

    Set<String> getRoles();

}
