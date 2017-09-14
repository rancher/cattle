package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;

import java.util.List;

public class LocalAuthTokenUtils extends AbstractTokenUtil {

    public LocalAuthTokenUtils(AuthDao authDao, TokenService tokenService, AuthTokenDao authTokenDao, ObjectManager objectManager, SettingDao settingsUtils,
            AccountDao accountDao) {
        super(authDao, tokenService, authTokenDao, objectManager, settingsUtils, accountDao);
    }

    @Override
    public String tokenType() {
        return LocalAuthConstants.JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        return true;
    }

    @Override
    protected String accessMode() {
        return LocalAuthConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return null;
    }

    @Override
    protected void postAuthModification(Account account) {
    }

    @Override
    public String userType() {
        return ProjectConstants.RANCHER_ID;
    }

    @Override
    public boolean createAccount() {
        return false;
    }

    @Override
    public String getName() {
        return LocalAuthConstants.CONFIG;
    }
}
