package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.util.ConstantsUtils;

public class AccountConstants {

    public static final String REGISTERED_AGENT_KIND = "registeredAgent";
    public static final String AGENT_KIND = "agent";
    public static final String SERVICE_KIND = "service";
    public static final String USER_KIND = "user";
    public static final String ADMIN_KIND = "admin";
    public static final String PROJECT_KIND = "project";

    public static final String ACCOUNT_ID = ConstantsUtils.property(Account.class, "accountId");
    public static final String SYSTEM_UUID = "system";

    public static final String OPTION_CREATE_APIKEY = "createApiKey";
    public static final String OPTION_CREATE_APIKEY_KIND = "createApiKeyKind";

    public static final String FIELD_DEFAULT_NETWORK_ID = "defaultNetworkId";

    public static final String DATA_ACT_AS_RESOURCE_ACCOUNT = "actAsResourceAccount";

    public static final String ACCOUNT_DEACTIVATE = "account.deactivate";
    public static final String ACCOUNT_REMOVE = "account.remove";

    public static final String AUTH_TYPE = "authType";
}
