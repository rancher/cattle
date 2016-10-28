package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.ldap.LDAPUtils;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class OpenLDAPConfigManager extends AbstractNoOpResourceManager {


    @Inject
    SettingsUtils settingsUtils;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{OpenLDAPConfig.class};
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(OpenLDAPConstants.CONFIG, request.getType())) {
            return null;
        }
        LDAPConstants config = request.proxyRequestObject(LDAPConstants.class);
        LDAPUtils.validateConfig(config);
        return updateCurrentConfig(config);
    }

    private OpenLDAPConfig currentLdapConfig(LDAPConstants config) {
        OpenLDAPConfig currentConfig = (OpenLDAPConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.getDomain() != null) {
            domain = config.getDomain();
        }
        String server = currentConfig.getServer();
        if (config.getServer() != null) {
            server = config.getServer();
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.getLoginDomain() != null) {
            loginDomain = config.getLoginDomain();
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.getAccessMode() != null) {
            accessMode = config.getAccessMode();
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.getServiceAccountUsername() != null) {
            serviceAccountUsername = config.getServiceAccountUsername();
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.getServiceAccountPassword() != null) {
            serviceAccountPassword = config.getServiceAccountPassword();
        }
        boolean tls = currentConfig.getTls();
        if (config.getTls() != null) {
            tls = config.getTls();
        }
        int port = currentConfig.getPort();
        if (config.getPort() != null) {
            port = config.getPort();
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.getEnabled() != null) {
            enabled = config.getEnabled();
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.getUserSearchField() != null){
            userSearchField = config.getUserSearchField();
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.getGroupSearchField() != null){
            groupSearchField = config.getGroupSearchField();
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.getUserLoginField() != null){
            userLoginField = config.getUserLoginField();
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.getUserDisabledBitMask() !=null){
            userEnabledMaskBit = config.getUserDisabledBitMask();
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.getUserObjectClass() != null) {
            userObjectClass = config.getUserObjectClass();
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.getUserNameField() != null){
            userNameField = config.getUserNameField();
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.getUserEnabledAttribute() != null){
            userEnabledAttribute = config.getUserEnabledAttribute();
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.getGroupObjectClass()!= null){
            groupObjectClass = config.getGroupObjectClass();
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.getGroupNameField() != null){
            groupNameField = config.getGroupNameField();
        }
        String userMemberAttribute = currentConfig.getUserMemberAttribute();
        if (config.getUserMemberAttribute() != null){
            userMemberAttribute = config.getUserMemberAttribute();
        }
        String groupMemberMappingAttribute = currentConfig.getGroupMemberMappingAttribute();
        if (config.getGroupMemberMappingAttribute() != null){
            groupMemberMappingAttribute = config.getGroupMemberMappingAttribute();
        }
        String groupDNField = currentConfig.getGroupDNField();
        if (config.getGroupDNField() != null){
            groupDNField = config.getGroupDNField();
        }
        String groupMemberUserAttribute = currentConfig.getGroupMemberUserAttribute();
        if (config.getGroupMemberUserAttribute() != null){
            groupMemberUserAttribute = config.getGroupMemberUserAttribute();
        }

        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute,
                groupMemberMappingAttribute, config.getConnectionTimeout(), groupDNField, groupMemberUserAttribute);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SecurityConstants.SECURITY.get();
        boolean tls = OpenLDAPConstants.TLS_ENABLED.get();
        int port = OpenLDAPConstants.LDAP_PORT.get();
        int userEnabledMaskBit = OpenLDAPConstants.USER_DISABLED_BIT_MASK.get();
        String accessMode = OpenLDAPConstants.ACCESS_MODE.get();
        String domain = OpenLDAPConstants.LDAP_DOMAIN.get();
        String groupMemberMappingAttribute = OpenLDAPConstants.GROUP_MEMBER_MAPPING_ATTRIBUTE.get();
        String groupNameField = OpenLDAPConstants.GROUP_NAME_FIELD.get();
        String groupObjectClass = OpenLDAPConstants.GROUP_OBJECT_CLASS.get();
        String groupSearchField = OpenLDAPConstants.GROUP_SEARCH_FIELD.get();
        String loginDomain = OpenLDAPConstants.LDAP_LOGIN_DOMAIN.get();
        String server = OpenLDAPConstants.LDAP_SERVER.get();
        String serviceAccountPassword = OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD.get();
        String serviceAccountUsername = OpenLDAPConstants.SERVICE_ACCOUNT_USER.get();
        String userEnabledAttribute = OpenLDAPConstants.USER_ENABLED_ATTRIBUTE.get();
        String userLoginField = OpenLDAPConstants.USER_LOGIN_FIELD.get();
        String userMemberAttribute = OpenLDAPConstants.USER_MEMBER_ATTRIBUTE.get();
        String userNameField = OpenLDAPConstants.USER_NAME_FIELD.get();
        String userObjectClass = OpenLDAPConstants.USER_OBJECT_CLASS.get();
        String userSearchField = OpenLDAPConstants.USER_SEARCH_FIELD.get();
        long connectionTimeout = OpenLDAPConstants.CONNECTION_TIMEOUT.get();
        String groupDNField = OpenLDAPConstants.GROUP_DN_FIELD.get();
        String groupMemberUserAttribute = OpenLDAPConstants.GROUP_MEMBER_USER_ATTRIBUTE.get();

        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute,
                groupMemberMappingAttribute, connectionTimeout, groupDNField, groupMemberUserAttribute);
    }

    public OpenLDAPConfig updateCurrentConfig(LDAPConstants config) {
        settingsUtils.changeSetting(OpenLDAPConstants.ACCESS_MODE_SETTING, config.getAccessMode());
        settingsUtils.changeSetting(OpenLDAPConstants.DOMAIN_SETTING, config.getDomain());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_NAME_FIELD_SETTING, config.getGroupNameField());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_OBJECT_CLASS_SETTING, config.getGroupObjectClass());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_SEARCH_FIELD_SETTING, config.getGroupSearchField());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_USER_MAPPING_ATTRIBUTE_SETTING, config.getGroupMemberMappingAttribute());
        settingsUtils.changeSetting(OpenLDAPConstants.LOGIN_DOMAIN_SETTING, config.getLoginDomain());
        settingsUtils.changeSetting(OpenLDAPConstants.PORT_SETTING, config.getPort());
        settingsUtils.changeSetting(OpenLDAPConstants.SERVER_SETTING, config.getServer());
        settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.getServiceAccountPassword());
        settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.getServiceAccountUsername());
        settingsUtils.changeSetting(OpenLDAPConstants.TLS_SETTING, config.getTls());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_DISABLED_BIT_MASK_SETTING, config.getUserDisabledBitMask());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.getUserEnabledAttribute());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_LOGIN_FIELD_SETTING, config.getUserLoginField());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_SETTING, config.getUserMemberAttribute());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_NAME_FIELD_SETTING, config.getUserNameField());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_OBJECT_CLASS_SETTING, config.getUserObjectClass());
        settingsUtils.changeSetting(OpenLDAPConstants.USER_SEARCH_FIELD_SETTING, config.getUserSearchField());
        settingsUtils.changeSetting(OpenLDAPConstants.TIMEOUT_SETTING, config.getConnectionTimeout());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_DN_FIELD_SETTING, config.getGroupDNField());
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_MEMBER_USER_ATTRIBUTE_SETTING, config.getGroupMemberUserAttribute());
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.getEnabled());
        if (config.getEnabled() != null){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, OpenLDAPConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        return currentLdapConfig(config);
    }

    public String getName() {
        return OpenLDAPConstants.MANAGER;
    }

}
