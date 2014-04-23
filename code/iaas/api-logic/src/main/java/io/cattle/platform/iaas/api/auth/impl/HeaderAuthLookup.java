package io.cattle.platform.iaas.api.auth.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class HeaderAuthLookup implements AccountLookup, Priority {

    public static final DynamicBooleanProperty ENABLED = ArchaiusUtil.getBoolean("api.auth.header.enabled");
    private static final String HEADER = "X-Account-Uuid";

    ObjectManager objectManager;

    @Override
    public Account getAccount(ApiRequest request) {
        if ( ! ENABLED.get() ) {
            return null;
        }

        String header = request.getServletContext().getRequest().getHeader(HEADER);

        if ( header == null ) {
            return null;
        }

        return objectManager.findOne(Account.class,
                ACCOUNT.UUID, header,
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE);
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
}
