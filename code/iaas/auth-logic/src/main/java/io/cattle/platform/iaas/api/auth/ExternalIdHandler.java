package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.model.Account;

import java.util.Set;

public interface ExternalIdHandler {

    ExternalId transform(ExternalId externalId);
    
    ExternalId untransform(ExternalId externalId);

    /**
     * Should never return null. If there are no externalIds then return and Empty set.
     * @param account The account to get the externalIds of.
     * @return Set of ExternalIds
     */
    Set<ExternalId> getExternalIds(Account account);

}
