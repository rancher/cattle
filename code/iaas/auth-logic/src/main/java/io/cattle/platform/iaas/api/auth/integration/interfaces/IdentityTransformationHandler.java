package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;

import java.util.Set;

public interface IdentityTransformationHandler extends Configurable, Scoped {

    Identity transform(Identity identity);

    Identity untransform(Identity identity);

    /**
     * Should never return null. If there are no identities then return and Empty set.
     *
     * @param account The account to get the identities of.
     * @return Set of Identities
     */
    Set<Identity> getIdentities(Account account);

}
