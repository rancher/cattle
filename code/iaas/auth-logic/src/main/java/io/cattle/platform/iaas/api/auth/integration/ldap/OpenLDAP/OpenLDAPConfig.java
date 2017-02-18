package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConfig;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = OpenLDAPConstants.CONFIG)
public class OpenLDAPConfig implements Configurable, LDAPConfig {

    private final Boolean enabled;
    private final String server;
    private final Integer port;
    private final Integer userDisabledBitMask;
    private final String loginDomain;
    private final String domain;
    private final String accessMode;
    private final String serviceAccountUsername;
    private final String serviceAccountPassword;
    private final Boolean tls;
    private final String userSearchField;
    private final String userLoginField;
    private final String userObjectClass;
    private final String userNameField;
    private final String userEnabledAttribute;
    private final String groupSearchField;
    private final String groupObjectClass;
    private final String groupNameField;
    private final String groupDNField;
    private final String userMemberAttribute;
    private final String groupMemberMappingAttribute;
    private final long connectionTimeout;
    private final String groupMemberUserAttribute;
    private List<Identity> allowedIdentities;

    public OpenLDAPConfig(String server, Integer port, Integer userDisabledBitMask, String loginDomain, String domain,
                          Boolean enabled, String accessMode, String serviceAccountUsername,
                          String serviceAccountPassword, Boolean tls, String userSearchField, String userLoginField,
                          String userObjectClass, String userNameField, String userEnabledAttribute, 
                          String groupSearchField, String groupObjectClass, String groupNameField, 
                          String userMemberAttribute, String groupMemberMappingAttribute, 
                          long connectionTimeout, String groupDNField, String groupMemberUserAttribute, List<Identity> allowedIdentities) {
        this.server = server;
        this.port = port;
        this.userDisabledBitMask = userDisabledBitMask;
        this.loginDomain = loginDomain;
        this.domain = domain;
        this.enabled = enabled;
        this.accessMode = accessMode;
        this.serviceAccountUsername = serviceAccountUsername;
        this.serviceAccountPassword = serviceAccountPassword;
        this.tls = tls;
        this.userSearchField = userSearchField;
        this.userLoginField = userLoginField;
        this.userObjectClass = userObjectClass;
        this.userNameField = userNameField;
        this.userEnabledAttribute = userEnabledAttribute;
        this.groupSearchField = groupSearchField;
        this.groupObjectClass = groupObjectClass;
        this.groupNameField = groupNameField;
        this.userMemberAttribute = userMemberAttribute;
        this.groupMemberMappingAttribute = groupMemberMappingAttribute;
        this.connectionTimeout = connectionTimeout;
        this.groupDNField = groupDNField;
        this.groupMemberUserAttribute = groupMemberUserAttribute;
        this.allowedIdentities = allowedIdentities;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getServer() {
        return server;
    }

    @Field(nullable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = false, required = true, defaultValue = "389")
    public Integer getPort() {
        return port;
    }

    @Field(required = false, nullable = true)
    public String getLoginDomain() {
        return loginDomain;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getDomain() {
        return domain;
    }

    @Field(required = true, nullable = false, defaultValue = "unrestricted")
    public String getAccessMode() {
        return accessMode;
    }

    @Override
    public boolean isConfigured() {
        return enabled;
    }

    @Override
    public String getName() {
        return OpenLDAPConstants.CONFIG;
    }

    @Field(nullable = true, required = true, minLength = 1)
    public String getServiceAccountUsername() {
        return serviceAccountUsername;
    }

    @Field(nullable = true, required = true, minLength = 1)
    public String getServiceAccountPassword() {
        return serviceAccountPassword;
    }

    @Field(nullable = false, required = true)
    public Boolean getTls() {
        return tls;
    }

    @Field(nullable = false, required = true, defaultValue = "uid")
    public String getUserSearchField() {
        return userSearchField;
    }

    @Field(nullable = false, required = true, defaultValue = "cn")
    public String getGroupSearchField() {
        return groupSearchField;
    }

    @Field(nullable = false, required = true, defaultValue = "uid")
    public String getUserLoginField() {
        return userLoginField;
    }

    @Field(nullable = false, required = true, defaultValue = "inetOrgPerson")
    public String getUserObjectClass() {
        return userObjectClass;
    }

    @Field(nullable = true, required = false, defaultValue = "")
    public String getUserEnabledAttribute() {
        return userEnabledAttribute;
    }

    @Field(nullable = false, required = true, defaultValue = "givenName")
    public String getUserNameField() {
        return userNameField;
    }

    @Field(nullable = true, required = false, defaultValue = "")
    public Integer getUserDisabledBitMask() {
        return userDisabledBitMask;
    }

    @Field(nullable = false, required = true, defaultValue = "posixGroup")
    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    @Field(nullable = false, required = true, defaultValue = "cn")
    public String getGroupNameField() {
        return groupNameField;
    }

    @Field(nullable = false, required = true, defaultValue = "memberOf")
    public String getUserMemberAttribute() {
        return userMemberAttribute;
    }

    @Field(nullable = false, required = true, defaultValue = "memberUid")
    public String getGroupMemberMappingAttribute() {
        return groupMemberMappingAttribute;
    }

    @Field(nullable = false, required = true, defaultValue = "1000")
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public List<Identity> getAllowedIdentities() {
        return allowedIdentities;
    }
    
    @Override
    @Field(nullable = false, required = false, defaultValue = "entryDN")
    public String getGroupDNField() {
        return groupDNField;
    }

    @Override
    @Field(nullable = false, required = false, defaultValue = "entryDN")
    public String getGroupMemberUserAttribute() {
        return groupMemberUserAttribute;
    }
}
