package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class RancherIdentityProvider implements IdentityProvider {

    @Inject
    AuthDao authDao;
    @Inject
    ObjectManager objectManager;

    @Inject
    IdFormatter idFormatter;

    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        List<Identity> identities = new ArrayList<>();
        if (!scopes().contains(scope)){
            return identities;
        }
        List<Account> accounts = new ArrayList<>();
        if (exactMatch){
            accounts.add(authDao.getByUsername(name));
        } else {
            accounts.addAll(authDao.searchUsers(name));
        }
        for(Account account: accounts){
            if (account != null) {
                identities.add(authDao.getIdentity(account.getId(), ApiContext.getContext().getIdFormatter()));
            }
        }
        return identities;
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        Set<Identity> identities = new HashSet<>();
        identities.add(new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId())));
        return identities;
    }

    @Override
    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopes()) {
            identities.addAll(searchIdentities(name, scope, exactMatch));
        }
        return identities;
    }

    private void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "RancherIdentityNotConfigured", "Rancher is not configured as an Identity provider.", null);
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        if (!scopes().contains(scope)) {
            return null;
        }
        String accountId = idFormatter.parseId(id);
        return authDao.getIdentity(Long.valueOf(accountId == null ? id : accountId),
                idFormatter);
    }

    @Override
    public Identity transform(Identity identity) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
        switch (identity.getExternalIdType()) {

            case ProjectConstants.RANCHER_ID:
                String accountId = idFormatter.parseId(identity.getExternalId());
                return authDao.getIdentity(Long.valueOf(accountId != null ? accountId: identity.getExternalId()), null);
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
                    return null;
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Rancher does not provide: " + identity.getExternalIdType(), null);
        }
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
        return ProjectConstants.RANCHER_SEARCH_PROVIDER;
    }

    @Override
    public String providerType() {
        return "rancherServer";
    }
}
