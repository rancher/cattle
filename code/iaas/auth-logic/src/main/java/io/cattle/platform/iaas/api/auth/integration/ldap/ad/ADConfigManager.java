package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
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

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(ADConstants.CONFIG, request.getType())) {
            return null;
        }

        LDAPConstants config = request.proxyRequestObject(LDAPConstants.class);
        LDAPUtils.validateConfig(config);
        return updateCurrentConfig(config);
    }

    private ADConfig currentLdapConfig(LDAPConstants config) {
        ADConfig currentConfig = (ADConfig) listInternal(null, null, null, null);
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

    public ADConfig updateCurrentConfig(LDAPConstants config) {
        settingsUtils.changeSetting(ADConstants.ACCESS_MODE_SETTING, config.getAccessMode());
        settingsUtils.changeSetting(ADConstants.DOMAIN_SETTING, config.getDomain());
        settingsUtils.changeSetting(ADConstants.GROUP_NAME_FIELD_SETTING, config.getGroupNameField());
        settingsUtils.changeSetting(ADConstants.GROUP_OBJECT_CLASS_SETTING, config.getGroupObjectClass());
        settingsUtils.changeSetting(ADConstants.GROUP_SEARCH_FIELD_SETTING, config.getGroupSearchField());
        settingsUtils.changeSetting(ADConstants.LOGIN_DOMAIN_SETTING, config.getLoginDomain());
        settingsUtils.changeSetting(ADConstants.PORT_SETTING, config.getPort());
        settingsUtils.changeSetting(ADConstants.SERVER_SETTING, config.getServer());
        settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.getServiceAccountPassword());
        settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.getServiceAccountUsername());
        settingsUtils.changeSetting(ADConstants.TLS_SETTING, config.getTls());
        settingsUtils.changeSetting(ADConstants.USER_DISABLED_BIT_MASK_SETTING, config.getUserDisabledBitMask());
        settingsUtils.changeSetting(ADConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.getUserEnabledAttribute());
        settingsUtils.changeSetting(ADConstants.USER_LOGIN_FIELD_SETTING, config.getUserLoginField());
        settingsUtils.changeSetting(ADConstants.USER_NAME_FIELD_SETTING, config.getUserNameField());
        settingsUtils.changeSetting(ADConstants.USER_OBJECT_CLASS_SETTING, config.getUserObjectClass());
        settingsUtils.changeSetting(ADConstants.USER_SEARCH_FIELD_SETTING, config.getUserSearchField());
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.getEnabled());
        if (config.getEnabled() != null){
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
