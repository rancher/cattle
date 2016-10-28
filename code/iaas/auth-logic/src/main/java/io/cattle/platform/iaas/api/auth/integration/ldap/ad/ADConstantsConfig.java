package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ADConstantsConfig extends ADConfigurable implements LDAPConstants{
    @Override
    public String getAccessMode() {
        return ADConstants.ACCESS_MODE.get();
    }

    @Override
    public String getName() {
        return "ADConstantsConfig";
    }

    @Override
    public String getServiceAccountUsername() {
        return ADConstants.SERVICE_ACCOUNT_USER.get();
    }

    @Override
    public String getServiceAccountPassword() {
        return ADConstants.SERVICE_ACCOUNT_PASSWORD.get();
    }

    @Override
    public String getServer() {
        return ADConstants.LDAP_SERVER.get();
    }

    @Override
    public Integer getPort() {
        return ADConstants.LDAP_PORT.get();
    }

    @Override
    public Boolean getTls() {
        return ADConstants.TLS_ENABLED.get();
    }

    @Override
    public String getLoginDomain() {
        return ADConstants.LDAP_LOGIN_DOMAIN.get();
    }

    @Override
    public String getDomain() {
        return ADConstants.LDAP_DOMAIN.get();
    }

    @Override
    public String getUserSearchField() {
        return ADConstants.USER_SEARCH_FIELD.get();
    }

    @Override
    public String getGroupSearchField() {
        return ADConstants.GROUP_SEARCH_FIELD.get();
    }

    @Override
    public String getUserLoginField() {
        return ADConstants.USER_LOGIN_FIELD.get();
    }

    @Override
    public String getUserObjectClass() {
        return ADConstants.USER_OBJECT_CLASS.get();
    }

    @Override
    public String getUserEnabledAttribute() {
        return ADConstants.USER_ENABLED_ATTRIBUTE.get();
    }

    @Override
    public String getUserNameField() {
        return ADConstants.USER_NAME_FIELD.get();
    }

    @Override
    public Integer getUserDisabledBitMask() {
        return ADConstants.USER_DISABLED_BIT_MASK.get();
    }

    @Override
    public String getGroupObjectClass() {
        return ADConstants.GROUP_OBJECT_CLASS.get();
    }

    @Override
    public String getGroupNameField() {
        return ADConstants.GROUP_NAME_FIELD.get();
    }

    @Override
    public String getUserScope() {
        return ADConstants.USER_SCOPE;
    }

    @Override
    public String getGroupScope() {
        return ADConstants.GROUP_SCOPE;
    }

    @Override
    public String getConfig() {
        return ADConstants.CONFIG;
    }

    @Override
    public String getJWTType() {
        return ADConstants.LDAP_JWT;
    }

    @Override
    public String getProviderName() {
        return ADConstants.NAME;
    }

    @Override
    public long getConnectionTimeout() {
        return ADConstants.CONNECTION_TIMEOUT.get();
    }

    @Override
    public String getUserMemberAttribute() {
        return ADConstants.MEMBER_OF;
    }

    @Override
    public String objectClass() {
        return ADConstants.OBJECT_CLASS;
    }

    @Override
    public String getGroupMemberMappingAttribute() {
        return ADConstants.MEMBER_OF;
    }

    @Override
    public Set<String> scopes() {
        return ADConstants.SCOPES;
    }

    @Override
    public List<Identity> getAllowedIdentities() {
        return new ArrayList<>();
    }

    @Override
    public String getGroupDNField() {
        return ADConstants.GROUP_DN_FIELD.get();
    }

    @Override
    public String getGroupMemberUserAttribute() {
        return ADConstants.GROUP_MEMBER_USER_ATTRIBUTE.get();
    }
}
