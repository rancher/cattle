package io.cattle.platform.api.account;

import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class AccountOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof Account)) {
            return converted;
        }

        Account account = (Account)original;
        if (account.getClusterOwner()) {
            converted.getLinks().remove("remove");
        }

        return converted;
    }
}
