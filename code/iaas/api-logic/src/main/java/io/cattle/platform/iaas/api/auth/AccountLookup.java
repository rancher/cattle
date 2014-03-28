package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AccountLookup {

    Account getAccount(ApiRequest request);

    void challenge(ApiRequest request);

}
