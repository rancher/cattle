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
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Set;
import javax.inject.Inject;

public class RancherIdentityTransformationHandler implements IdentityTransformationHandler {

    @Inject
    AuthDao authDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    RancherIdentitySearchProvider rancherIdentitySearchProvider;

    @Override
    public Identity transform(Identity identity) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
        switch (identity.getExternalIdType()) {

            case ProjectConstants.RANCHER_ID:
                String accountId = idFormatter.parseId(identity.getExternalId());
                Identity gotIdentity = authDao.getIdentity(Long.valueOf(accountId), null);
                if (gotIdentity != null) {
                    return gotIdentity;
                } else {
                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                            "invalidIdentity", "Rancher Account: " + identity.getExternalId() + " nonexistent", null);
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Rancher does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
        switch (identity.getExternalIdType()) {
            case ProjectConstants.RANCHER_ID:
                long id;
                try {
                    id = Long.valueOf(identity.getExternalId());
                } catch (NumberFormatException e) {
                    id = Long.valueOf(idFormatter.parseId(identity.getExternalId()));
                }
                Identity gotIdentity = authDao.getIdentity(id, idFormatter);
                if (gotIdentity != null) {
                    return new Identity(gotIdentity, identity.getRole(), identity.getProjectId());
                } else {
                    String accountId = (String) idFormatter.formatId(objectManager.getType(Account.class), id);
                    return new Identity(identity.getExternalIdType(), accountId, identity.getName(),
                            null, null, '(' + identity.getExternalIdType().split("_")[1].toUpperCase()
                            +  "  not found) " + identity.getName());
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Rancher does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        return rancherIdentitySearchProvider.getIdentities(account);
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
