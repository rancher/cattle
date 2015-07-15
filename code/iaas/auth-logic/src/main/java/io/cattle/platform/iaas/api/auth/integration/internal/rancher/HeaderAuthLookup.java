package io.cattle.platform.iaas.api.auth.integration.internal.rancher;


import static io.cattle.platform.core.model.tables.AccountTable.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashSet;
import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class HeaderAuthLookup implements AccountLookup, Priority {

    public static final DynamicBooleanProperty ENABLED = ArchaiusUtil.getBoolean("api.auth.header.enabled");
    private static final String HEADER = "X-Account-Uuid";

    ObjectManager objectManager;
    AccountLookup adminLookup;
    AuthorizationProvider adminAuthProvider;

    @Override
    public Account getAccount(ApiRequest request) {
        if (!ENABLED.get()) {
            return null;
        }

        String header = request.getServletContext().getRequest().getHeader(HEADER);

        if (header == null) {
            return null;
        }

        Account admin = adminLookup.getAccount(request);
        if (admin == null) {
            return null;
        }

        Policy policy = adminAuthProvider.getPolicy(admin, admin, new HashSet<Identity>(), request);
        if (!policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS)) {
            return null;
        }
        Account account = objectManager.findOne(Account.class, ACCOUNT.UUID, header, ACCOUNT.STATE, CommonStatesConstants.ACTIVE);
        return account;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AccountLookup getAdminLookup() {
        return adminLookup;
    }

    @Inject
    public void setAdminLookup(AccountLookup adminLookup) {
        this.adminLookup = adminLookup;
    }

    public AuthorizationProvider getAdminAuthProvider() {
        return adminAuthProvider;
    }

    @Inject
    public void setAdminAuthProvider(AuthorizationProvider adminAuthProvider) {
        this.adminAuthProvider = adminAuthProvider;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getName() {
        return "HeaderAuthLookUp";
    }
}
