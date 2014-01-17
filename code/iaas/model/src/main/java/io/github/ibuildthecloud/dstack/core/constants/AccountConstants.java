package io.github.ibuildthecloud.dstack.core.constants;

import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.util.ConstantsUtils;

public class AccountConstants {

    public static final String AGENT_KIND = "agent";

    public static final String ACCOUNT_ID = ConstantsUtils.property(Account.class, "accountId");
    public static final String SYSTEM_UUID = "system";

}
