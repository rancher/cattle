package io.cattle.platform.iaas.api.auth.integration.ldap.ad;


import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConfig;
import io.cattle.platform.api.auth.Identity;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = ADConstants.CONFIG)
public class ADConfig implements Configurable, LDAPConfig {

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
    private final String groupMemberUserAttribute;
    private final long connectionTimeout;
    private List<Identity> allowedIdentities;

    public ADConfig(String server, Integer port, Integer userDisabledBitMask, String loginDomain, String domain,
                    Boolean enabled, String accessMode, String serviceAccountUsername,
                    String serviceAccountPassword, Boolean tls, String userSearchField, String userLoginField,
                    String userObjectClass, String userNameField, String userEnabledAttribute, String groupSearchField,
                    String groupObjectClass, String groupNameField, long connectionTimeout, 
                    List<Identity> allowedIdentities, String groupDNField, String groupMemberUserAttribute) {
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
        this.connectionTimeout = connectionTimeout;
        this.allowedIdentities = allowedIdentities;
        this.groupDNField = groupDNField;
        this.groupMemberUserAttribute = groupMemberUserAttribute;
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

    @Override
    @Field(required = false, nullable = true)
    public String getLoginDomain() {
        return loginDomain;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getDomain() {
        return domain;
    }

    @Override
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
        return ADConstants.CONFIG;
    }

    @Field(nullable = false, required = true, minLength = 1)
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

    @Override
    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getUserSearchField() {
        return userSearchField;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getGroupSearchField() {
        return groupSearchField;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getUserLoginField() {
        return userLoginField;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "person")
    public String getUserObjectClass() {
        return userObjectClass;
    }

    @Override
    @Field(nullable = false, required = false, defaultValue = "userAccountControl")
    public String getUserEnabledAttribute() {
        return userEnabledAttribute;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "name")
    public String getUserNameField() {
        return userNameField;
    }

    @Override
    @Field(nullable = false, required = false, defaultValue = "2")
    public Integer getUserDisabledBitMask() {
        return userDisabledBitMask;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "group")
    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "name")
    public String getGroupNameField() {
        return groupNameField;
    }

    @Override
    @Field(nullable = true, required = false, defaultValue = "memberOf")
    public String getUserMemberAttribute() {
        return ADConstants.MEMBER_OF;
    }

    @Override
    @Field(nullable = true, required = false, defaultValue = "memberOf")
    public String getGroupMemberMappingAttribute() {
        return ADConstants.MEMBER_OF;
    }

    @Override
    @Field(nullable = false, required = true, defaultValue = "1000")
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    @Field(nullable = true)
    public List<Identity> getAllowedIdentities() {
        return allowedIdentities;
    }

    @Override
    @Field(nullable = false, required = false, defaultValue = "distinguishedName")
    public String getGroupDNField() {
        return groupDNField;
    }

    @Override
    @Field(nullable = false, required = false, defaultValue = "distinguishedName")
    public String getGroupMemberUserAttribute() {
        return groupMemberUserAttribute;
    }
}
