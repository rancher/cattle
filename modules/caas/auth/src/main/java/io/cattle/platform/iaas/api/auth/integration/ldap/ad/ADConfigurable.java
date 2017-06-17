package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Provider;

import org.apache.commons.lang3.StringUtils;

public abstract class ADConfigurable implements Configurable, Provider {

    @Override
    public boolean isConfigured() {
        boolean allProps = StringUtils.isNotBlank(ADConstants.USER_LOGIN_FIELD.get()) &&
                StringUtils.isNotBlank(String.valueOf(ADConstants.LDAP_PORT.get())) &&
                StringUtils.isNotBlank(ADConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(ADConstants.SERVICE_ACCOUNT_USER.get()) &&
                StringUtils.isNotBlank(ADConstants.SERVICE_ACCOUNT_PASSWORD.get()) &&
                StringUtils.isNotBlank(ADConstants.USER_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(ADConstants.USER_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(String.valueOf(ADConstants.USER_DISABLED_BIT_MASK.get())) &&
                StringUtils.isNotBlank(ADConstants.USER_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(ADConstants.GROUP_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(ADConstants.GROUP_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(ADConstants.GROUP_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(ADConstants.LDAP_DOMAIN.get());
        return StringUtils.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get(), ADConstants.CONFIG) &&
                allProps;
    }

    @Override
    public String providerType() {
        return ADConstants.CONFIG;
    }

    public Boolean getEnabled(){
        return isConfigured();
    }
}
