package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = LdapConstants.CONFIG)
public class LdapConfig implements Configurable {

    private final boolean enabled;
    private final String server;
    private final int port;
    private final int userDisabledBitMask;
    private final String loginDomain;
    private final String domain;
    private final String accessMode;
    private final String serviceAccountUsername;
    private final String serviceAccountPassword;
    private final boolean tls;
    private final String userSearchField;
    private final String userLoginField;
    private final String userObjectClass;
    private final String userNameField;
    private final String userEnabledAttribute;
    private final String groupSearchField;
    private final String groupObjectClass;
    private final String groupNameField;

    public LdapConfig(String server, int port, int userDisabledBitMask, String loginDomain, String domain,
                      boolean enabled, String accessMode, String serviceAccountUsername,
                      String serviceAccountPassword, boolean tls, String userSearchField, String userLoginField,
                      String userObjectClass, String userNameField, String userEnabledAttribute, String groupSearchField,
                      String groupObjectClass, String groupNameField) {
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
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getServer() {
        return server;
    }

    @Field(nullable = false)
    public boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = false, required = true, defaultValue = "389")
    public int getPort() {
        return port;
    }

    @Field(required = true, nullable = false, minLength = 1)
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
        return LdapConstants.CONFIG;
    }

    @Field(nullable = false, required = true, minLength = 1)
    public String getServiceAccountUsername() {
        return serviceAccountUsername;
    }

    @Field(nullable = false, required = true, minLength = 1)
    public String getServiceAccountPassword() {
        return serviceAccountPassword;
    }

    @Field(nullable = false, required = true)
    public boolean getTls() {
        return tls;
    }

    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getUserSearchField() {
        return userSearchField;
    }

    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getGroupSearchField() {
        return groupSearchField;
    }

    @Field(nullable = false, required = true, defaultValue = "sAMAccountName")
    public String getUserLoginField() {
        return userLoginField;
    }

    @Field(nullable = false, required = true, defaultValue = "person")
    public String getUserObjectClass() {
        return userObjectClass;
    }

    @Field(nullable = false, required = true, defaultValue = "userAccountControl")
    public String getUserEnabledAttribute() {
        return userEnabledAttribute;
    }

    @Field(nullable = false, required = true, defaultValue = "name")
    public String getUserNameField() {
        return userNameField;
    }

    @Field(nullable = false, required = true, defaultValue = "2")
    public int getUserDisabledBitMask() {
        return userDisabledBitMask;
    }

    @Field(nullable = false, required = true, defaultValue = "group")
    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    @Field(nullable = false, required = true, defaultValue = "name")
    public String getGroupNameField() {
        return groupNameField;
    }
}
