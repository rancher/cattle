package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;

public abstract class LocalAuthConfigurable implements Configurable {

    @Override
    public boolean isConfigured() {
        return LocalAuthConstants.CONFIG.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get());
    }
}
