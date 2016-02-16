package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class AccountOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();

        Identity identity = null;
        if (original instanceof Account) {
            Account account = (Account) original;
            if (AccountConstants.PROJECT_KIND.equalsIgnoreCase(account.getKind())) {
                return converted;
            }
            if (account.getExternalId() != null && account.getExternalIdType() != null) {
                identity = new Identity(account.getExternalIdType(), account.getExternalId());
            } else {
                if (!AccountConstants.PROJECT_KIND.equalsIgnoreCase(account.getKind())) {
                    identity = new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()));
                }
            }
        }
        if (identity != null) {
            if (idFormatter != null) {
                converted.getFields().put("identity", idFormatter.formatId("identity", identity.getId()));
            } else {
                converted.getFields().put("identity", identity.getId());
            }
        }
        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Account.class };
    }
}
