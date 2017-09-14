package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Provider;

import org.apache.commons.lang3.StringUtils;

public abstract class OpenLDAPConfigurable implements Configurable, Provider {

    @Override
    public boolean isConfigured() {
        boolean allProps = StringUtils.isNotBlank(OpenLDAPConstants.USER_LOGIN_FIELD.get()) &&
                StringUtils.isNotBlank(String.valueOf(OpenLDAPConstants.LDAP_PORT.get())) &&
                StringUtils.isNotBlank(OpenLDAPConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.SERVICE_ACCOUNT_USER.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.USER_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.USER_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.USER_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.GROUP_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.GROUP_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.GROUP_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.GROUP_MEMBER_MAPPING_ATTRIBUTE.get()) &&
                StringUtils.isNotBlank(OpenLDAPConstants.LDAP_DOMAIN.get());
        return StringUtils.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get(), OpenLDAPConstants.CONFIG) &&
                allProps;
    }

    @Override
    public String providerType() {
        return OpenLDAPConstants.CONFIG;
    }

    public Boolean getEnabled(){
        return isConfigured();
    }
}
