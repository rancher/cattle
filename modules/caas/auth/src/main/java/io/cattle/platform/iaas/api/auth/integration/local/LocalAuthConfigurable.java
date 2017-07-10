package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Provider;

public abstract class LocalAuthConfigurable implements Configurable, Provider {

    @Override
    public boolean isConfigured() {
        return LocalAuthConstants.CONFIG.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get());
    }

    @Override
    public String providerType() {
        return LocalAuthConstants.CONFIG;
    }
}
