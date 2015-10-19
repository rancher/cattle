package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentityTransformationHandler;

import java.util.List;
import javax.inject.Inject;

public class LocalAuthUtils extends TokenUtils {

    @Inject
    RancherIdentityTransformationHandler rancherIdentityTransformationHandler;

    @Override
    protected String getAccountType() {
        return ProjectConstants.RANCHER_ID;
    }

    @Override
    protected String tokenType() {
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
}
