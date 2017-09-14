package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Provider;


import org.apache.commons.lang3.StringUtils;

public abstract class AzureConfigurable implements Configurable, Provider{

    @Override
    public boolean isConfigured() {
        return StringUtils.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get(), AzureConstants.CONFIG) &&
                StringUtils.isNotBlank(AzureConstants.AZURE_CLIENT_ID.get()) &&
                StringUtils.isNotBlank(AzureConstants.AZURE_TENANT_ID.get()) && 
                StringUtils.isNotBlank(AzureConstants.AZURE_DOMAIN.get());
    }

    @Override
    public String providerType() {
        return AzureConstants.CONFIG;
    }
}
