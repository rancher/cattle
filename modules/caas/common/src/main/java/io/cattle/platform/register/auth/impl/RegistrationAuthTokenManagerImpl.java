package io.cattle.platform.register.auth.impl;

import io.cattle.platform.core.addon.RegistrationToken;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.RegistrationTokenAuthDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.register.auth.RegistrationAuthTokenManager;

import java.util.Date;

public class RegistrationAuthTokenManagerImpl implements RegistrationAuthTokenManager {

    RegistrationTokenAuthDao authDao;
    ObjectManager objectManager;
    AccountDao accountDao;

    public RegistrationAuthTokenManagerImpl(RegistrationTokenAuthDao authDao, ObjectManager objectManager, AccountDao accountDao) {
        this.authDao = authDao;
        this.objectManager = objectManager;
        this.accountDao = accountDao;
    }

    @Override
    public TokenAccount validateToken(String password) {
        String[] parts = password.split(":");

        if (parts.length != 3) {
            return null;
        }

        Date date = null;

        try {
            long time = Long.parseLong(parts[1]);

            if (System.currentTimeMillis() > (time + RegistrationToken.getAllowedTime())) {
                return null;
            }

            date = new Date(time);
        } catch (NumberFormatException e) {
            return null;
        }

        Credential cred = authDao.getCredential(parts[0]);

        if (cred == null) {
            return null;
        }

        String token = RegistrationToken.createToken(cred.getPublicValue(), cred.getSecretValue(), date);

        if (!password.equals(token)) {
            return null;
        }

        Account account = objectManager.loadResource(Account.class, cred.getAccountId());

        if (account == null || !accountDao.isActiveAccount(account) || account.getRemoved() != null) {
            return null;
        }

        return new TokenAccount(account, DataAccessor.fieldLong(cred, CredentialConstants.FIELD_CLUSTER_ID));
    }

}
