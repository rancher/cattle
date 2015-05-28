package io.cattle.platform.iaas.api.auth;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AccountLookup {

    AccountAccess getAccountAccess(ApiRequest request);

    boolean challenge(ApiRequest request);

}
