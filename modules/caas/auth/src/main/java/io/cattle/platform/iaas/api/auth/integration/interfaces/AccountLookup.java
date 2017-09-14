package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AccountLookup extends Configurable{

    Account getAccount(ApiRequest request);

    boolean challenge(ApiRequest request);

}
