package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

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

public class ADConfigManager extends AbstractNoOpResourceManager {

    @Inject
    SettingsUtils settingsUtils;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ADIdentityProvider adIdentityProvider;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{ADConfig.class};
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(ADConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        LDAPUtils.validateConfig(currentLdapConfig(config));
        return updateCurrentConfig(config);
    }

    private ADConfig currentLdapConfig(Map<String, Object> config) {
        ADConfig currentConfig = (ADConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(ADConstants.DOMAIN) != null) {
            domain = (String) config.get(ADConstants.DOMAIN);
        }
        String server = currentConfig.getServer();
        if (config.get(ADConstants.SERVER) != null) {
            server = (String) config.get(ADConstants.SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(ADConstants.LOGIN_DOMAIN) != null) {
            loginDomain = (String) config.get(ADConstants.LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(ADConstants.ACCESSMODE) != null) {
            accessMode = (String) config.get(ADConstants.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(ADConstants.SERVICE_ACCOUNT_USERNAME_FIELD) != null) {
            serviceAccountUsername = (String) config.get(ADConstants.SERVICE_ACCOUNT_USERNAME_FIELD);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(ADConstants.SERVICE_ACCOUNT_PASSWORD_FIELD) != null) {
            serviceAccountPassword = (String) config.get(ADConstants.SERVICE_ACCOUNT_PASSWORD_FIELD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(ADConstants.TLS) != null) {
            tls = (Boolean) config.get(ADConstants.TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(ADConstants.PORT) != null) {
            port = (int) (long) config.get(ADConstants.PORT);
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(ADConstants.USER_SEARCH_FIELD_FIELD) != null){
            userSearchField = (String) config.get(ADConstants.USER_SEARCH_FIELD_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(ADConstants.GROUP_SEARCH_FIELD_FIELD) != null){
            groupSearchField = (String) config.get(ADConstants.GROUP_SEARCH_FIELD_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(ADConstants.USER_LOGIN_FIELD_FIELD) != null){
            userLoginField = (String) config.get(ADConstants.USER_LOGIN_FIELD_FIELD);
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.get(ADConstants.USER_DISABLED_MASK_BIT) !=null){
            userEnabledMaskBit = (int) (long) config.get(ADConstants.USER_DISABLED_MASK_BIT);
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.get(ADConstants.USER_OBJECT_CLASS_FIELD) != null) {
            userObjectClass = (String) config.get(ADConstants.USER_OBJECT_CLASS_FIELD);
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.get(ADConstants.USER_NAME_FIELD_FIELD) != null){
            userNameField = (String) config.get(ADConstants.USER_NAME_FIELD_FIELD);
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.get(ADConstants.USER_ENABLED_ATTRIBUTE_FIELD) != null){
            userEnabledAttribute = (String) config.get(ADConstants.USER_ENABLED_ATTRIBUTE_FIELD);
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.get(ADConstants.GROUP_OBJECT_CLASS_FIELD) != null){
            groupObjectClass = (String) config.get(ADConstants.GROUP_OBJECT_CLASS_FIELD);
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.get(ADConstants.GROUP_NAME_FIELD_FIELD) != null){
            groupNameField = (String) config.get(ADConstants.GROUP_NAME_FIELD_FIELD);
        }
        return new ADConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField,
                userObjectClass, userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SecurityConstants.SECURITY.get();
        boolean tls = ADConstants.TLS_ENABLED.get();

        String server = ADConstants.LDAP_SERVER.get();
        String loginDomain = ADConstants.LDAP_LOGIN_DOMAIN.get();
        String domain = ADConstants.LDAP_DOMAIN.get();
        String accessMode = ADConstants.ACCESS_MODE.get();
        String serviceAccountPassword = ADConstants.SERVICE_ACCOUNT_PASSWORD.get();
        String serviceAccountUsername = ADConstants.SERVICE_ACCOUNT_USER.get();
        String userSearchField = ADConstants.USER_SEARCH_FIELD.get();
        String groupSearchField = ADConstants.GROUP_SEARCH_FIELD.get();
        String userLoginField = ADConstants.USER_LOGIN_FIELD.get();
        int port = ADConstants.LDAP_PORT.get();
        int userEnabledMaskBit = ADConstants.USER_DISABLED_BIT_MASK.get();
        String userObjectClass = ADConstants.USER_OBJECT_CLASS.get();
        String userNameField = ADConstants.USER_NAME_FIELD.get();
        String groupObjectClass = ADConstants.GROUP_OBJECT_CLASS.get();
        String userEnabledAttribute = ADConstants.USER_ENABLED_ATTRIBUTE.get();
        String groupNameField = ADConstants.GROUP_NAME_FIELD.get();
        return new ADConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField);
    }

    public ADConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(ADConstants.DOMAIN_SETTING, config.get(ADConstants.DOMAIN));
        settingsUtils.changeSetting(ADConstants.ACCESS_MODE_SETTING, config.get(ADConstants.ACCESSMODE));
        settingsUtils.changeSetting(ADConstants.SERVER_SETTING, config.get(ADConstants.SERVER));
        settingsUtils.changeSetting(ADConstants.LOGIN_DOMAIN_SETTING, config.get(ADConstants.LOGIN_DOMAIN));
        settingsUtils.changeSetting(ADConstants.USER_SEARCH_FIELD_SETTING, config.get(ADConstants.USER_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(ADConstants.GROUP_SEARCH_FIELD_SETTING, config.get(ADConstants.GROUP_SEARCH_FIELD_FIELD));
        settingsUtils.changeSetting(ADConstants.USER_LOGIN_FIELD_SETTING, config.get(ADConstants.USER_LOGIN_FIELD_FIELD));
        settingsUtils.changeSetting(ADConstants.PORT_SETTING, config.get(ADConstants.PORT));
        settingsUtils.changeSetting(ADConstants.USER_DISABLED_BIT_MASK_SETTING, config.get(ADConstants.USER_DISABLED_MASK_BIT));
        settingsUtils.changeSetting(ADConstants.USER_OBJECT_CLASS_SETTING, config.get(ADConstants.USER_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(ADConstants.USER_NAME_FIELD_SETTING, config.get(ADConstants.USER_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(ADConstants.GROUP_OBJECT_CLASS_SETTING, config.get(ADConstants.GROUP_OBJECT_CLASS_FIELD));
        settingsUtils.changeSetting(ADConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.get(ADConstants.USER_ENABLED_ATTRIBUTE_FIELD));
        settingsUtils.changeSetting(ADConstants.GROUP_NAME_FIELD_SETTING, config.get(ADConstants.GROUP_NAME_FIELD_FIELD));
        settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.get(ADConstants.SERVICE_ACCOUNT_USERNAME_FIELD));
        settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(ADConstants.SERVICE_ACCOUNT_PASSWORD_FIELD));
        settingsUtils.changeSetting(ADConstants.TLS_SETTING, config.get(ADConstants.TLS));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));
        if (config.get(SecurityConstants.ENABLED) != null){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, ADConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        return currentLdapConfig(config);
    }

    public String getName() {
        return ADConstants.MANAGER;
    }

}
