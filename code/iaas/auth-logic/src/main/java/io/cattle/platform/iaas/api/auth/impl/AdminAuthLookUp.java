package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;

public class AdminAuthLookUp implements AccountLookup, Priority {

    private static final String ENFORCE_AUTH_HEADER = "X-ENFORCE-AUTHENTICATION";
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");

    @Inject
    AuthDao authDao;

    @Override
    public Account getAccount(ApiRequest request) {
        if (SECURITY.get()){
            return null;
        }
        String authHeader = StringUtils.trim(request.getServletContext().getRequest().getHeader(ENFORCE_AUTH_HEADER));
        if (StringUtils.equals("true", authHeader)) {
            return null;
        }
        return authDao.getAdminAccount();
    }
    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
