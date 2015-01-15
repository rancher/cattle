package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.util.ConstantsUtils;

public class AccountConstants {

    public static final String AGENT_KIND = "agent";

    public static final String ACCOUNT_ID = ConstantsUtils.property(Account.class, "accountId");
    public static final String SYSTEM_UUID = "system";

    public static final String OPTION_CREATE_APIKEY = "createApiKey";
    public static final String OPTION_CREATE_APIKEY_KIND = "createApiKeyKind";

    public static final String FIELD_DEFAULT_CREDENTIAL_IDS = "defaultCredentialIds";
    public static final String FIELD_DEFAULT_NETWORK_IDS = "defaultNetworkIds";
    
    public static final String PROJECT = "project"; 

}
