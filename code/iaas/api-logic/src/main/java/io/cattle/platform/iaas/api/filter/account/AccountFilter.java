package io.cattle.platform.iaas.api.filter.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;
import javax.inject.Inject;

public class AccountFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    AccountDao accountDao;

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        long accountId = Long.valueOf(id);
        Account anAdminAccount = accountDao.getAdminAccountExclude(accountId);
        if (anAdminAccount == null) {
            Account accountToEdit = accountDao.getAccountById(accountId);
            Map<String, Object> requestObject = CollectionUtils.toMap(request.getRequestObject());
            boolean hasAdmin = requestObject.containsKey("kind");
            if (AccountConstants.ADMIN_KIND.equalsIgnoreCase(accountToEdit.getKind()) &&
                    hasAdmin && !AccountConstants.ADMIN_KIND.equalsIgnoreCase(String.valueOf(requestObject.get("kind")))) {
                throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, AccountConstants.LAST_ADMIN_ACCOUNT,
                        "Cannot change the last admin account to not be admin.", AccountConstants.ADMIN_REQUIRED_MESSAGE);
            }

        }
        return super.update(type, id, request, next);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        long accountId = Long.valueOf(id);
        Account anAdminAccount = accountDao.getAdminAccountExclude(accountId);
        if (anAdminAccount == null) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, AccountConstants.LAST_ADMIN_ACCOUNT,
                    "Cannot delete the last admin account.", AccountConstants.ADMIN_REQUIRED_MESSAGE);
        }
        return super.delete(type, id, request, next);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Account.class };
    }
}
