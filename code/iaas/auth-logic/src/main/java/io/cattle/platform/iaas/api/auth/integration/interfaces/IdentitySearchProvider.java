package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.api.auth.Identity;

import java.util.List;

public interface IdentitySearchProvider extends Configurable, Scoped {

    List<Identity> searchIdentities(String name, boolean exactMatch);

    List<Identity> searchIdentities(String name, String scope, boolean exactMatch);

    Identity getIdentity(String id, String scope);

}
