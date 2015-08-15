package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

public class RancherIdentityTransformationHandler implements IdentityTransformationHandler {

    @Inject
    AuthDao authDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Identity transform(Identity identity) {
        switch (identity.getExternalIdType()) {

            case ProjectConstants.RANCHER_ID:
                String accountId = ApiContext.getContext().getIdFormatter().parseId(identity.getExternalId());
                Account account = authDao.getAccountById(Long.valueOf(accountId));
                if (account != null) {
                    return new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName(),
                        null, null, null);
                } else {
                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                            "invalidIdentity", "Rancher Account" + identity.getExternalId() + "nonexistent", null);
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Rancher does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {

            case ProjectConstants.RANCHER_ID:
                Account account;
                try {
                    account = authDao.getAccountById(Long.valueOf(identity.getExternalId()));
                } catch (NumberFormatException e) {
                    return identity;
                }
                if (account != null) {
                    String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager
                            .getType(Account.class), account.getId());
                    return new Identity(ProjectConstants.RANCHER_ID, accountId, account.getName(), null, null, null);
                } else {
                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                            "invalidIdentity", "Rancher Account" + identity.getExternalId() + "nonexistent", null);
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Rancher does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        Set<Identity> identities = new HashSet<>();
        identities.add(new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName(),
                null, null, null));
        return identities;
    }

    @Override
    public Set<String> scopes() {
        return ProjectConstants.SCOPES;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getName() {
        return "RancherIdentityTransformationHandler";
    }
}
