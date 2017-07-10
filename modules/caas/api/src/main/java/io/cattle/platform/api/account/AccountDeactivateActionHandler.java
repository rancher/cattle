package io.cattle.platform.api.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class AccountDeactivateActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;
    AccountDao accountDao;

    public AccountDeactivateActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager, AccountDao accountDao) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
        this.accountDao = accountDao;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (obj instanceof Account) {
            Account accountToDeactivate = (Account) obj;
            if (AccountConstants.ADMIN_KIND.equalsIgnoreCase(accountToDeactivate.getKind())) {
                Account anAdminAccount = accountDao.getAdminAccountExclude(accountToDeactivate.getId());
                if (anAdminAccount == null) {
                    throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, AccountConstants.LAST_ADMIN_ACCOUNT,
                            "Cannot deactivate the last admin account.", AccountConstants.ADMIN_REQUIRED_MESSAGE);
                }
            } else if(AccountConstants.PROJECT_KIND.equalsIgnoreCase(accountToDeactivate.getKind())) {
                accountToDeactivate = accountDao.getAccountById(accountToDeactivate.getId());
                if (accountToDeactivate == null) {
                    throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
                }
            }
            objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, accountToDeactivate, null);
            accountToDeactivate = objectManager.reload(accountToDeactivate);
            return accountToDeactivate;
        } else {
            return null;
        }
    }

}
