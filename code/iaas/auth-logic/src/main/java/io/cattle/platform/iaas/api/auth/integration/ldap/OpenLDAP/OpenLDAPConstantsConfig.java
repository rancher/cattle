package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpenLDAPConstantsConfig extends OpenLDAPConfigurable implements LDAPConstants{
    @Override
    public String getAccessMode() {
        return OpenLDAPConstants.ACCESS_MODE.get();
    }

    @Override
    public String getName() {
        return "OpenLDAPConstantsConfig";
    }

    @Override
    public String getServiceAccountUsername() {
        return OpenLDAPConstants.SERVICE_ACCOUNT_USER.get();
    }

    @Override
    public String getServiceAccountPassword() {
        return OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD.get();
    }

    @Override
    public String getServer() {
        return OpenLDAPConstants.LDAP_SERVER.get();
    }

    @Override
    public Integer getPort() {
        return OpenLDAPConstants.LDAP_PORT.get();
    }

    @Override
    public Boolean getTls() {
        return OpenLDAPConstants.TLS_ENABLED.get();
    }

    @Override
    public String getLoginDomain() {
        return OpenLDAPConstants.LDAP_LOGIN_DOMAIN.get();
    }

    @Override
    public String getDomain() {
        return OpenLDAPConstants.LDAP_DOMAIN.get();
    }

    @Override
    public String getUserSearchField() {
        return OpenLDAPConstants.USER_SEARCH_FIELD.get();
    }

    @Override
    public String getGroupSearchField() {
        return OpenLDAPConstants.GROUP_SEARCH_FIELD.get();
    }

    @Override
    public String getUserLoginField() {
        return OpenLDAPConstants.USER_LOGIN_FIELD.get();
    }

    @Override
    public String getUserObjectClass() {
        return OpenLDAPConstants.USER_OBJECT_CLASS.get();
    }

    @Override
    public String getUserEnabledAttribute() {
        return OpenLDAPConstants.USER_ENABLED_ATTRIBUTE.get();
    }

    @Override
    public String getUserNameField() {
        return OpenLDAPConstants.USER_NAME_FIELD.get();
    }

    @Override
    public Integer getUserDisabledBitMask() {
        return OpenLDAPConstants.USER_DISABLED_BIT_MASK.get();
    }

    @Override
    public String getGroupObjectClass() {
        return OpenLDAPConstants.GROUP_OBJECT_CLASS.get();
    }

    @Override
    public String getGroupNameField() {
        return OpenLDAPConstants.GROUP_NAME_FIELD.get();
    }

    @Override
    public String getUserScope() {
        return OpenLDAPConstants.USER_SCOPE;
    }

    @Override
    public String getGroupScope() {
        return OpenLDAPConstants.GROUP_SCOPE;
    }

    @Override
    public String getConfig() {
        return OpenLDAPConstants.CONFIG;
    }

    @Override
    public String getJWTType() {
        return OpenLDAPConstants.LDAP_JWT;
    }

    @Override
    public String getProviderName() {
        return OpenLDAPConstants.NAME;
    }

    @Override
    public long getConnectionTimeout() {
        return OpenLDAPConstants.CONNECTION_TIMEOUT.get();
    }

    @Override
    public String getUserMemberAttribute() {
        return OpenLDAPConstants.USER_MEMBER_ATTRIBUTE.get();
    }

    @Override
    public String objectClass() {
        return OpenLDAPConstants.OBJECT_CLASS;
    }

    @Override
    public String getGroupMemberMappingAttribute() {
        return OpenLDAPConstants.GROUP_MEMBER_MAPPING_ATTRIBUTE.get();
    }

    @Override
    public Set<String> scopes() {
        return OpenLDAPConstants.SCOPES;
    }

    @Override
    public List<Identity> getAllowedIdentities() {
        return new ArrayList<>();
    }

    @Override
    public String getGroupDNField() {
        return OpenLDAPConstants.GROUP_DN_FIELD.get();
    }

    @Override
    public String getGroupMemberUserAttribute() {
        return OpenLDAPConstants.GROUP_MEMBER_USER_ATTRIBUTE.get();
    }
}
