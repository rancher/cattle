package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;

import java.util.List;

public class LocalAuthTokenUtils extends AbstractTokenUtil {

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
