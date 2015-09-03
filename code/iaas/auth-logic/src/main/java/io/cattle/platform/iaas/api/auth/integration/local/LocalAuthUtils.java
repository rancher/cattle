package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;

import java.util.List;

public class LocalAuthUtils extends TokenUtils {
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
}
