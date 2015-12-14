package io.cattle.platform.iaas.api.auth.integration.ldap.interfaces;

import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Provider;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Scoped;

public interface LDAPConstants extends LDAPConfig, Configurable, Scoped, Provider{

    Boolean getEnabled();

    String objectClass();

    String getUserScope();

    String getGroupScope();

    String getConfig();

    String getJWTType();

    String getProviderName();
}
