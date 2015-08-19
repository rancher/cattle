package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;

import java.util.List;
import java.util.Set;

public interface IdentitySearchProvider extends Configurable, Scoped {

    List<Identity> searchIdentities(String name, boolean exactMatch);

    List<Identity> searchIdentities(String name, String scope, boolean exactMatch);

    Set<Identity> getIdentities(Account account);

    Identity getIdentity(String id, String scope);

}
