package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.ldap.LDAPUtils;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.List;
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
        Map<String, Object> configMap = CollectionUtils.toMap(request.getRequestObject());
        return updateCurrentConfig(configMap);
    }

    @SuppressWarnings("unchecked")
    private ADConfig currentLdapConfig(Map<String, Object> config) {
        ADConfig currentConfig = (ADConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(ADConstants.CONFIG_DOMAIN) != null) {
            domain = (String)config.get(ADConstants.CONFIG_DOMAIN);
        }
        String server = currentConfig.getServer();
        if (config.get(ADConstants.CONFIG_SERVER) != null) {
            server = (String)config.get(ADConstants.CONFIG_SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(ADConstants.CONFIG_LOGIN_DOMAIN) != null) {
            loginDomain = (String)config.get(ADConstants.CONFIG_LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(AbstractTokenUtil.ACCESSMODE) != null) {
            accessMode = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_USERNAME) != null) {
            serviceAccountUsername = (String)config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_USERNAME);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD) != null) {
            serviceAccountPassword = (String)config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(ADConstants.CONFIG_TLS) != null) {
            tls = (Boolean)config.get(ADConstants.CONFIG_TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(ADConstants.CONFIG_PORT) != null) {
            port = ((Long)config.get(ADConstants.CONFIG_PORT)).intValue();
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(ADConstants.CONFIG_SECURITY) != null) {
            enabled = (Boolean)config.get(ADConstants.CONFIG_SECURITY);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(ADConstants.CONFIG_USER_SEARCH_FIELD) != null){
            userSearchField = (String)config.get(ADConstants.CONFIG_USER_SEARCH_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(ADConstants.CONFIG_GROUP_SEARCH_FIELD) != null){
            groupSearchField = (String)config.get(ADConstants.CONFIG_GROUP_SEARCH_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(ADConstants.CONFIG_USER_LOGIN_FIELD) != null){
            userLoginField = (String)config.get(ADConstants.CONFIG_USER_LOGIN_FIELD);
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.get(ADConstants.CONFIG_USER_DISABLED_BIT_MASK) !=null){
            userEnabledMaskBit = ((Long)config.get(ADConstants.CONFIG_USER_DISABLED_BIT_MASK)).intValue();
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.get(ADConstants.CONFIG_USER_OBJECT_CLASS) != null) {
            userObjectClass = (String)config.get(ADConstants.CONFIG_USER_OBJECT_CLASS);
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.get(ADConstants.CONFIG_USER_NAME_FIELD) != null){
            userNameField = (String)config.get(ADConstants.CONFIG_USER_NAME_FIELD);
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.get(ADConstants.CONFIG_USER_ENABLED_ATTRIBUTE) != null){
            userEnabledAttribute = (String)config.get(ADConstants.CONFIG_USER_ENABLED_ATTRIBUTE);
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.get(ADConstants.CONFIG_GROUP_OBJECT_CLASS)!= null){
            groupObjectClass = (String)config.get(ADConstants.CONFIG_GROUP_OBJECT_CLASS);
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.get(ADConstants.CONFIG_GROUP_NAME_FIELD) != null){
            groupNameField = (String)config.get(ADConstants.CONFIG_GROUP_NAME_FIELD);
        }
        List<Identity> identities = currentConfig.getAllowedIdentities();

        String accessModeInConfig = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        if (config.get(ADConstants.CONFIG_ALLOWED_IDENTITIES) != null && accessModeInConfig != null 
                && (AbstractTokenUtil.isRestrictedAccess(accessModeInConfig) || AbstractTokenUtil.isRequiredAccess(accessModeInConfig))) {
            identities = adIdentityProvider.getIdentities((List<Map<String, String>>) config.get(ADConstants.CONFIG_ALLOWED_IDENTITIES));
        }

        String groupDNField = currentConfig.getGroupDNField();
        if (config.get(ADConstants.CONFIG_GROUP_DN_FIELD) != null){
            groupDNField = (String)config.get(ADConstants.CONFIG_GROUP_DN_FIELD);
        }

        String groupMemberUserAttribute = currentConfig.getGroupMemberUserAttribute();
        if (config.get(ADConstants.CONFIG_GROUP_MEMBER_USER_ATTRIBUTE) != null){
            groupMemberUserAttribute = (String)config.get(ADConstants.CONFIG_GROUP_MEMBER_USER_ATTRIBUTE);
        }

        return new ADConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField,
                userObjectClass, userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField,
                (Long)config.get(ADConstants.CONFIG_TIMEOUT), identities, groupDNField, groupMemberUserAttribute);
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
        long connectionTimeout = ADConstants.CONNECTION_TIMEOUT.get();
        List<Identity> identities = adIdentityProvider.savedIdentities();
        String groupDNField = ADConstants.GROUP_DN_FIELD.get();
        String groupMemberUserAttribute = ADConstants.GROUP_MEMBER_USER_ATTRIBUTE.get();

        return new ADConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField,
                connectionTimeout, identities, groupDNField, groupMemberUserAttribute);
    }

    public ADConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(ADConstants.ACCESS_MODE_SETTING, config.get(AbstractTokenUtil.ACCESSMODE));
        settingsUtils.changeSetting(ADConstants.DOMAIN_SETTING, config.get(ADConstants.CONFIG_DOMAIN));
        settingsUtils.changeSetting(ADConstants.GROUP_NAME_FIELD_SETTING, config.get(ADConstants.CONFIG_GROUP_NAME_FIELD));
        settingsUtils.changeSetting(ADConstants.GROUP_OBJECT_CLASS_SETTING, config.get(ADConstants.CONFIG_GROUP_OBJECT_CLASS));
        settingsUtils.changeSetting(ADConstants.GROUP_SEARCH_FIELD_SETTING, config.get(ADConstants.CONFIG_GROUP_SEARCH_FIELD));
        settingsUtils.changeSetting(ADConstants.LOGIN_DOMAIN_SETTING, config.get(ADConstants.CONFIG_LOGIN_DOMAIN));
        settingsUtils.changeSetting(ADConstants.PORT_SETTING, config.get(ADConstants.CONFIG_PORT));
        settingsUtils.changeSetting(ADConstants.SERVER_SETTING, config.get(ADConstants.CONFIG_SERVER));
        if(config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD) != null){
            settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD));
        }
        settingsUtils.changeSetting(ADConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.get(ADConstants.CONFIG_SERVICE_ACCOUNT_USERNAME));
        settingsUtils.changeSetting(ADConstants.TLS_SETTING, config.get(ADConstants.CONFIG_TLS));
        settingsUtils.changeSetting(ADConstants.USER_DISABLED_BIT_MASK_SETTING, config.get(ADConstants.CONFIG_USER_DISABLED_BIT_MASK));
        settingsUtils.changeSetting(ADConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.get(ADConstants.CONFIG_USER_ENABLED_ATTRIBUTE));
        settingsUtils.changeSetting(ADConstants.USER_LOGIN_FIELD_SETTING, config.get(ADConstants.CONFIG_USER_LOGIN_FIELD));
        settingsUtils.changeSetting(ADConstants.USER_NAME_FIELD_SETTING, config.get(ADConstants.CONFIG_USER_NAME_FIELD));
        settingsUtils.changeSetting(ADConstants.USER_OBJECT_CLASS_SETTING, config.get(ADConstants.CONFIG_USER_OBJECT_CLASS));
        settingsUtils.changeSetting(ADConstants.USER_SEARCH_FIELD_SETTING, config.get(ADConstants.CONFIG_USER_SEARCH_FIELD));
        settingsUtils.changeSetting(ADConstants.TIMEOUT_SETTING, config.get(ADConstants.CONFIG_TIMEOUT));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(ADConstants.CONFIG_SECURITY));

        if (config.get(ADConstants.CONFIG_SECURITY) != null){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, ADConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }

        String accessModeInConfig = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        if (AbstractTokenUtil.isRestrictedAccess(accessModeInConfig) || AbstractTokenUtil.isRequiredAccess(accessModeInConfig)) {
            //validate the allowedIdentities
            @SuppressWarnings("unchecked")
            String ids = adIdentityProvider.validateIdentities((List<Map<String, String>>) config.get(ADConstants.CONFIG_ALLOWED_IDENTITIES));
            settingsUtils.changeSetting(ADConstants.ALLOWED_IDENTITIES_SETTING, ids);
        } else if (AbstractTokenUtil.isUnrestrictedAccess(accessModeInConfig)) {
            //clear out the allowedIdentities Set
            settingsUtils.changeSetting(ADConstants.ALLOWED_IDENTITIES_SETTING, null);
        }

        return currentLdapConfig(config);
    }

    public String getName() {
        return ADConstants.MANAGER;
    }

}
