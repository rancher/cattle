package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LdapConfigManager extends AbstractNoOpResourceManager {

    @Inject
    SettingsUtils settingsUtils;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{LdapConfig.class};
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(LdapConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        LDAPUtils.validateConfig(currentLdapConfig(config));
        return updateCurrentConfig(config);
    }

    private LdapConfig currentLdapConfig(Map<String, Object> config) {
        LdapConfig currentConfig = (LdapConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(LdapConstants.DOMAIN) != null) {
            domain = (String) config.get(LdapConstants.DOMAIN);
        }
        String server = currentConfig.getServer();
        if (config.get(LdapConstants.SERVER) != null) {
            server = (String) config.get(LdapConstants.SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(LdapConstants.LOGIN_DOMAIN) != null) {
            loginDomain = (String) config.get(LdapConstants.LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(LdapConstants.ACCESSMODE) != null) {
            loginDomain = (String) config.get(LdapConstants.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME_FIELD) != null) {
            loginDomain = (String) config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME_FIELD);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD_FIELD) != null) {
            loginDomain = (String) config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD_FIELD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(LdapConstants.TLS) != null) {
            tls = (Boolean) config.get(LdapConstants.TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(LdapConstants.PORT) != null) {
            port = (int) (long) config.get(LdapConstants.PORT);
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(LdapConstants.USER_SEARCH_FIELD_FIELD) != null){
            userSearchField = (String) config.get(LdapConstants.USER_SEARCH_FIELD_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(LdapConstants.GROUP_SEARCH_FIELD_FIELD) != null){
            groupSearchField = (String) config.get(LdapConstants.GROUP_SEARCH_FIELD_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(LdapConstants.USER_LOGIN_FIELD_FIELD) != null){
            groupSearchField = (String) config.get(LdapConstants.USER_LOGIN_FIELD_FIELD);
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.get(LdapConstants.USER_DISABLED_MASK_BIT) !=null){
            userEnabledMaskBit = (int) (long) config.get(LdapConstants.USER_DISABLED_MASK_BIT);
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.get(LdapConstants.USER_OBJECT_CLASS_FIELD) != null) {
            userObjectClass = (String) config.get(LdapConstants.USER_OBJECT_CLASS_FIELD);
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.get(LdapConstants.USER_NAME_FIELD_FIELD) != null){
            userNameField = (String) config.get(LdapConstants.USER_NAME_FIELD_FIELD);
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.get(LdapConstants.USER_ENABLED_ATTRIBUTE_FIELD) != null){
            userEnabledAttribute = (String) config.get(LdapConstants.USER_ENABLED_ATTRIBUTE_FIELD);
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.get(LdapConstants.GROUP_OBJECT_CLASS_FIELD) != null){
            groupObjectClass = (String) config.get(LdapConstants.GROUP_OBJECT_CLASS_FIELD);
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.get(LdapConstants.GROUP_NAME_FIELD_FIELD) != null){
            groupNameField = (String) config.get(LdapConstants.GROUP_NAME_FIELD_FIELD);
        }
        return new LdapConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField,
                userObjectClass, userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SecurityConstants.SECURITY.get();
        boolean tls = LdapConstants.TLS_ENABLED.get();

        String server = LdapConstants.LDAP_SERVER.get();
        String loginDomain = LdapConstants.LDAP_LOGIN_DOMAIN.get();
        String domain = LdapConstants.LDAP_DOMAIN.get();
        String accessMode = LdapConstants.ACCESS_MODE.get();
        String serviceAccountPassword = LdapConstants.SERVICE_ACCOUNT_PASSWORD.get();
        String serviceAccountUsername = LdapConstants.SERVICE_ACCOUNT_USER.get();
        String userSearchField = LdapConstants.USER_SEARCH_FIELD.get();
        String groupSearchField = LdapConstants.GROUP_SEARCH_FIELD.get();
        String userLoginField = LdapConstants.USER_LOGIN_FIELD.get();
        int port = LdapConstants.LDAP_PORT.get();
        int userEnabledMaskBit = LdapConstants.USER_DISABLED_BIT_MASK.get();
        String userObjectClass = LdapConstants.USER_OBJECT_CLASS.get();
        String userNameField = LdapConstants.USER_NAME_FIELD.get();
        String groupObjectClass = LdapConstants.GROUP_OBJECT_CLASS.get();
        String userEnabledAttribute = LdapConstants.USER_ENABLED_ATTRIBUTE.get();
        String groupNameField = LdapConstants.GROUP_NAME_FIELD.get();
        return new LdapConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField);
    }

    public LdapConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(LdapConstants.DOMAIN_SETTING, config.get(LdapConstants.DOMAIN));
        settingsUtils.changeSetting(LdapConstants.ACCESS_MODE_SETTING, config.get(LdapConstants.ACCESSMODE));
        settingsUtils.changeSetting(LdapConstants.SERVER_SETTING, config.get(LdapConstants.SERVER));
        settingsUtils.changeSetting(LdapConstants.LOGIN_DOMAIN_SETTING, config.get(LdapConstants.LOGIN_DOMAIN));
        settingsUtils.changeSetting(LdapConstants.USER_SEARCH_FIELD_SETTING, config.get(LdapConstants.USER_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(LdapConstants.GROUP_SEARCH_FIELD_SETTING, config.get(LdapConstants.GROUP_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(LdapConstants.USER_LOGIN_FIELD_SETTING, config.get(LdapConstants.USER_LOGIN_FIELD_FIELD));
        settingsUtils.changeSetting(LdapConstants.PORT_SETTING, config.get(LdapConstants.PORT));
        settingsUtils.changeSetting(LdapConstants.USER_DISABLED_BIT_MASK_SETTING, config.get(LdapConstants.USER_DISABLED_MASK_BIT));
        settingsUtils.changeSetting(LdapConstants.USER_OBJECT_CLASS_SETTING, config.get(LdapConstants.USER_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(LdapConstants.USER_NAME_FIELD_SETTING, config.get(LdapConstants.USER_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(LdapConstants.GROUP_OBJECT_CLASS_SETTING, config.get(LdapConstants.GROUP_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(LdapConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.get(LdapConstants.USER_ENABLED_ATTRIBUTE_FIELD));
        settingsUtils.changeSetting(LdapConstants.GROUP_NAME_FIELD_SETTING, config.get(LdapConstants.GROUP_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(LdapConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME_FIELD));
        settingsUtils.changeSetting(LdapConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD_FIELD));
        settingsUtils.changeSetting(LdapConstants.TLS_SETTING, config.get(LdapConstants.TLS));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));
        if (config.get(SecurityConstants.ENABLED) != null){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, LdapConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        return currentLdapConfig(config);
    }

    public String getName() {
        return LdapConstants.MANAGER;
    }

}
