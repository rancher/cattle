package io.github.ibuildthecloud.dstack.iaas.api.auth;

import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AccountLookup {

    Account getAccount(ApiRequest request);

    void challenge(ApiRequest request);

}
