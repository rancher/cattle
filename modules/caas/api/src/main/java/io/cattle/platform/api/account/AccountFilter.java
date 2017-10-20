package io.cattle.platform.api.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.Tables.ACCOUNT;

public class AccountFilter extends AbstractValidationFilter {

    private static final String NAME_NOT_UNIQUE = "NameNotUnique";
    AccountDao accountDao;
    ObjectManager objectManager;

    public AccountFilter(AccountDao accountDao, ObjectManager objectManager) {
        this.accountDao = accountDao;
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String accountName = request.proxyRequestObject(Account.class).getName();
        Long clusterId = request.proxyRequestObject(Account.class).getClusterId();
        Account account = objectManager.findOne(Account.class, ACCOUNT.NAME, accountName, ACCOUNT.CLUSTER_ID, clusterId, ACCOUNT.REMOVED, null);
        if (account != null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, NAME_NOT_UNIQUE);
        }
        return super.create(type, request, next);
    }

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

        validateClusterId(id, request);

        return super.update(type, id, request, next);
    }

    protected void validateClusterId(String id, ApiRequest request) {
        Account account = objectManager.loadResource(Account.class, id);
        Long newClusterId = request.proxyRequestObject(Account.class).getClusterId();
        if (account != null && account.getClusterId() != null &&
                request.getRequestParams().containsKey(ObjectMetaDataManager.CLUSTER_FIELD) &&
                !Objects.equals(newClusterId, account.getClusterId())) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, AccountConstants.CLUSTER_ALREADY_SET,
                    "Cluster is already assigned", null);
        }
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        long accountId = Long.valueOf(id);
        Account anAdminAccount = accountDao.getAdminAccountExclude(accountId);
        if (anAdminAccount == null) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, AccountConstants.LAST_ADMIN_ACCOUNT,
                    "Cannot delete the last admin account.", AccountConstants.ADMIN_REQUIRED_MESSAGE);
        }

        Account account = objectManager.loadResource(Account.class, id);
        if (account.getClusterOwner()) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, AccountConstants.CANT_DELETE_SYSTEM_ENVIRONMENT,
                    "Cannot delete system environment", null);

        }
        return super.delete(type, id, request, next);
    }
}
