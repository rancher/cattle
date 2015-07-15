package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Set;
import javax.inject.Inject;

public class LdapIdentityTransformationHandler extends LdapConfigurable implements IdentityTransformationHandler {

    @Inject
    LdapIdentitySearchProvider ldapIdentitySearchProvider;

    @Inject
    LdapUtils ldapUtils;

    @Override
    public Identity transform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case LdapConstants.USER_SCOPE:
            case LdapConstants.GROUP_SCOPE:
                return ldapIdentitySearchProvider.getIdentity(identity.getExternalId(), identity.getExternalIdType());
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case LdapConstants.USER_SCOPE:
                break;
            case LdapConstants.GROUP_SCOPE:
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
        return identity;
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        return ldapUtils.getIdentities();
    }

    @Override
    public Set<String> scopes() {
        return LdapConstants.SCOPES;
    }

    @Override
    public String getName() {
        return "LdapIdentityTransformationHandler";
    }
}
