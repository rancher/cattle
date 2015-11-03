package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
import io.cattle.platform.iaas.api.auth.integration.ldap.LDAPUtils;
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


    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(OpenLDAPConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        LDAPUtils.validateConfig(currentLdapConfig(config));
        return updateCurrentConfig(config);
    }

    private OpenLDAPConfig currentLdapConfig(Map<String, Object> config) {
        OpenLDAPConfig currentConfig = (OpenLDAPConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(OpenLDAPConstants.DOMAIN) != null) {
            domain = (String) config.get(OpenLDAPConstants.DOMAIN);
        }
        String server = currentConfig.getServer();
        if (config.get(OpenLDAPConstants.SERVER) != null) {
            server = (String) config.get(OpenLDAPConstants.SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(OpenLDAPConstants.LOGIN_DOMAIN) != null) {
            loginDomain = (String) config.get(OpenLDAPConstants.LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(OpenLDAPConstants.ACCESSMODE) != null) {
            accessMode = (String) config.get(OpenLDAPConstants.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_FIELD) != null) {
            serviceAccountUsername = (String) config.get(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_FIELD);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_FIELD) != null) {
            serviceAccountPassword = (String) config.get(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_FIELD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(OpenLDAPConstants.TLS) != null) {
            tls = (Boolean) config.get(OpenLDAPConstants.TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(OpenLDAPConstants.PORT) != null) {
            port = (int) (long) config.get(OpenLDAPConstants.PORT);
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(OpenLDAPConstants.USER_SEARCH_FIELD_FIELD) != null){
            userSearchField = (String) config.get(OpenLDAPConstants.USER_SEARCH_FIELD_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(OpenLDAPConstants.GROUP_SEARCH_FIELD_FIELD) != null){
            groupSearchField = (String) config.get(OpenLDAPConstants.GROUP_SEARCH_FIELD_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(OpenLDAPConstants.USER_LOGIN_FIELD_FIELD) != null){
            userLoginField = (String) config.get(OpenLDAPConstants.USER_LOGIN_FIELD_FIELD);
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.get(OpenLDAPConstants.USER_DISABLED_MASK_BIT) !=null){
            userEnabledMaskBit = (int) (long) config.get(OpenLDAPConstants.USER_DISABLED_MASK_BIT);
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.get(OpenLDAPConstants.USER_OBJECT_CLASS_FIELD) != null) {
            userObjectClass = (String) config.get(OpenLDAPConstants.USER_OBJECT_CLASS_FIELD);
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.get(OpenLDAPConstants.USER_NAME_FIELD_FIELD) != null){
            userNameField = (String) config.get(OpenLDAPConstants.USER_NAME_FIELD_FIELD);
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.get(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_FIELD) != null){
            userEnabledAttribute = (String) config.get(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_FIELD);
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.get(OpenLDAPConstants.GROUP_OBJECT_CLASS_FIELD) != null){
            groupObjectClass = (String) config.get(OpenLDAPConstants.GROUP_OBJECT_CLASS_FIELD);
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.get(OpenLDAPConstants.GROUP_NAME_FIELD_FIELD) != null){
            groupNameField = (String) config.get(OpenLDAPConstants.GROUP_NAME_FIELD_FIELD);
        }
        String userMemberAttribute = currentConfig.getUserMemberAttribute();
        if (config.get(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_FIELD) != null){
            userMemberAttribute = (String) config.get(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_FIELD);
        }
        String groupMemberMappingAttribute = currentConfig.getGroupMemberMappingAttribute();
        if (config.get(OpenLDAPConstants.GROUP_MEMBER_MAPPING_FIELD_FIELD) != null){
            groupMemberMappingAttribute = (String) config.get(OpenLDAPConstants.GROUP_MEMBER_MAPPING_FIELD_FIELD);
        }
        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute, groupMemberMappingAttribute);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SecurityConstants.SECURITY.get();
        boolean tls = OpenLDAPConstants.TLS_ENABLED.get();

        String server = OpenLDAPConstants.LDAP_SERVER.get();
        String loginDomain = OpenLDAPConstants.LDAP_LOGIN_DOMAIN.get();
        String domain = OpenLDAPConstants.LDAP_DOMAIN.get();
        String accessMode = OpenLDAPConstants.ACCESS_MODE.get();
        String serviceAccountPassword = OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD.get();
        String serviceAccountUsername = OpenLDAPConstants.SERVICE_ACCOUNT_USER.get();
        String userSearchField = OpenLDAPConstants.USER_SEARCH_FIELD.get();
        String groupSearchField = OpenLDAPConstants.GROUP_SEARCH_FIELD.get();
        String userLoginField = OpenLDAPConstants.USER_LOGIN_FIELD.get();
        int port = OpenLDAPConstants.LDAP_PORT.get();
        int userEnabledMaskBit = OpenLDAPConstants.USER_DISABLED_BIT_MASK.get();
        String userObjectClass = OpenLDAPConstants.USER_OBJECT_CLASS.get();
        String userNameField = OpenLDAPConstants.USER_NAME_FIELD.get();
        String groupObjectClass = OpenLDAPConstants.GROUP_OBJECT_CLASS.get();
        String userEnabledAttribute = OpenLDAPConstants.USER_ENABLED_ATTRIBUTE.get();
        String groupNameField = OpenLDAPConstants.GROUP_NAME_FIELD.get();
        String userMemberAttribute = OpenLDAPConstants.USER_MEMBER_ATTRIBUTE.get();
        String groupMemberMappingAttribute = OpenLDAPConstants.GROUP_MEMBER_MAPPING_ATTRIBUTE.get();
        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute, groupMemberMappingAttribute);
    }

    public OpenLDAPConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(OpenLDAPConstants.DOMAIN_SETTING, config.get(OpenLDAPConstants.DOMAIN));
        settingsUtils.changeSetting(OpenLDAPConstants.ACCESS_MODE_SETTING, config.get(OpenLDAPConstants.ACCESSMODE));
        settingsUtils.changeSetting(OpenLDAPConstants.SERVER_SETTING, config.get(OpenLDAPConstants.SERVER));
        settingsUtils.changeSetting(OpenLDAPConstants.LOGIN_DOMAIN_SETTING, config.get(OpenLDAPConstants.LOGIN_DOMAIN));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_SEARCH_FIELD_SETTING, config.get(OpenLDAPConstants.USER_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_SEARCH_FIELD_SETTING, config.get(OpenLDAPConstants.GROUP_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_LOGIN_FIELD_SETTING, config.get(OpenLDAPConstants.USER_LOGIN_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.PORT_SETTING, config.get(OpenLDAPConstants.PORT));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_DISABLED_BIT_MASK_SETTING, config.get(OpenLDAPConstants.USER_DISABLED_MASK_BIT));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_OBJECT_CLASS_SETTING, config.get(OpenLDAPConstants.USER_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_NAME_FIELD_SETTING, config.get(OpenLDAPConstants.USER_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_USER_MAPPING_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.GROUP_MEMBER_MAPPING_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_OBJECT_CLASS_SETTING, config.get(OpenLDAPConstants.GROUP_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_NAME_FIELD_SETTING, config.get(OpenLDAPConstants.GROUP_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.get(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.TLS_SETTING, config.get(OpenLDAPConstants.TLS));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));
        if (config.get(SecurityConstants.ENABLED) != null){
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
