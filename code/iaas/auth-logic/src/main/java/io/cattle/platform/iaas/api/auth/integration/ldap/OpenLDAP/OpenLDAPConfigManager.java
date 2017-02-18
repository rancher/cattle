package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

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

public class OpenLDAPConfigManager extends AbstractNoOpResourceManager {


    @Inject
    SettingsUtils settingsUtils;

    @Inject
    OpenLDAPIdentityProvider openLDAPIdentityProvider;

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
        Map<String, Object> configMap = CollectionUtils.toMap(request.getRequestObject());
        return updateCurrentConfig(configMap);
    }

    @SuppressWarnings("unchecked")
    private OpenLDAPConfig currentLdapConfig(Map<String, Object> config) {
        OpenLDAPConfig currentConfig = (OpenLDAPConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(OpenLDAPConstants.CONFIG_DOMAIN) != null) {
            domain = (String)config.get(OpenLDAPConstants.CONFIG_DOMAIN);
        }
        String server = currentConfig.getServer();
        if (config.get(OpenLDAPConstants.CONFIG_SERVER) != null) {
            server = (String)config.get(OpenLDAPConstants.CONFIG_SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(OpenLDAPConstants.CONFIG_LOGIN_DOMAIN) != null) {
            loginDomain = (String)config.get(OpenLDAPConstants.CONFIG_LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(AbstractTokenUtil.ACCESSMODE) != null) {
            accessMode = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_USERNAME) != null) {
            serviceAccountUsername = (String)config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_USERNAME);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD) != null) {
            serviceAccountPassword = (String)config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(OpenLDAPConstants.CONFIG_TLS) != null) {
            tls = (Boolean)config.get(OpenLDAPConstants.CONFIG_TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(OpenLDAPConstants.CONFIG_PORT) != null) {
            port = ((Long)config.get(OpenLDAPConstants.CONFIG_PORT)).intValue();
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(OpenLDAPConstants.CONFIG_SECURITY) != null) {
            enabled = (Boolean)config.get(OpenLDAPConstants.CONFIG_SECURITY);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(OpenLDAPConstants.CONFIG_USER_SEARCH_FIELD) != null){
            userSearchField = (String)config.get(OpenLDAPConstants.CONFIG_USER_SEARCH_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(OpenLDAPConstants.CONFIG_GROUP_SEARCH_FIELD) != null){
            groupSearchField = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_SEARCH_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(OpenLDAPConstants.CONFIG_USER_LOGIN_FIELD) != null){
            userLoginField = (String)config.get(OpenLDAPConstants.CONFIG_USER_LOGIN_FIELD);
        }
        int userEnabledMaskBit = currentConfig.getUserDisabledBitMask();
        if (config.get(OpenLDAPConstants.CONFIG_USER_DISABLED_BIT_MASK) !=null){
            userEnabledMaskBit = ((Long)config.get(OpenLDAPConstants.CONFIG_USER_DISABLED_BIT_MASK)).intValue();
        }

        String userObjectClass = currentConfig.getUserObjectClass();
        if (config.get(OpenLDAPConstants.CONFIG_USER_OBJECT_CLASS) != null) {
            userObjectClass = (String)config.get(OpenLDAPConstants.CONFIG_USER_OBJECT_CLASS);
        }
        String userNameField = currentConfig.getUserNameField();
        if (config.get(OpenLDAPConstants.CONFIG_USER_NAME_FIELD) != null){
            userNameField = (String)config.get(OpenLDAPConstants.CONFIG_USER_NAME_FIELD);
        }
        String userEnabledAttribute = currentConfig.getUserEnabledAttribute();
        if (config.get(OpenLDAPConstants.CONFIG_USER_ENABLED_ATTRIBUTE) != null){
            userEnabledAttribute = (String)config.get(OpenLDAPConstants.CONFIG_USER_ENABLED_ATTRIBUTE);
        }
        String groupObjectClass  = currentConfig.getGroupObjectClass();
        if (config.get(OpenLDAPConstants.CONFIG_GROUP_OBJECT_CLASS)!= null){
            groupObjectClass = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_OBJECT_CLASS);
        }
        String groupNameField = currentConfig.getGroupNameField();
        if (config.get(OpenLDAPConstants.CONFIG_GROUP_NAME_FIELD) != null){
            groupNameField = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_NAME_FIELD);
        }
        String userMemberAttribute = currentConfig.getUserMemberAttribute();
        if (config.get(OpenLDAPConstants.CONFIG_USER_MEMBER_ATTRIBUTE) != null){
            userMemberAttribute = (String)config.get(OpenLDAPConstants.CONFIG_USER_MEMBER_ATTRIBUTE);
        }
        String groupMemberMappingAttribute = currentConfig.getGroupMemberMappingAttribute();
        if (config.get(OpenLDAPConstants.CONFIG_GROUP_USER_MAPPING_ATTRIBUTE) != null){
            groupMemberMappingAttribute = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_USER_MAPPING_ATTRIBUTE);
        }
        String groupDNField = currentConfig.getGroupDNField();
        if (config.get(OpenLDAPConstants.CONFIG_GROUP_DN_FIELD) != null){
            groupDNField = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_DN_FIELD);
        }
        String groupMemberUserAttribute = currentConfig.getGroupMemberUserAttribute();
        if(config.get(OpenLDAPConstants.CONFIG_GROUP_MEMBER_USER_ATTRIBUTE) != null){
            groupMemberUserAttribute = (String)config.get(OpenLDAPConstants.CONFIG_GROUP_MEMBER_USER_ATTRIBUTE);
        }

        List<Identity> identities = currentConfig.getAllowedIdentities();
        String accessModeInConfig = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        if (config.get(OpenLDAPConstants.CONFIG_ALLOWED_IDENTITIES) != null && accessModeInConfig != null
                && (AbstractTokenUtil.isRestrictedAccess(accessModeInConfig) || AbstractTokenUtil.isRequiredAccess(accessModeInConfig))) {
            identities = openLDAPIdentityProvider.getIdentities((List<Map<String, String>>) config.get(OpenLDAPConstants.CONFIG_ALLOWED_IDENTITIES));
        }

        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute,
                groupMemberMappingAttribute, (Long)config.get(OpenLDAPConstants.CONFIG_TIMEOUT), groupDNField, groupMemberUserAttribute, identities);
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
        List<Identity> identities = openLDAPIdentityProvider.savedIdentities();

        return new OpenLDAPConfig(server, port, userEnabledMaskBit, loginDomain, domain, enabled, accessMode,
                serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, userObjectClass,
                userNameField, userEnabledAttribute, groupSearchField, groupObjectClass, groupNameField, userMemberAttribute,
                groupMemberMappingAttribute, connectionTimeout, groupDNField, groupMemberUserAttribute, identities);
    }

    public OpenLDAPConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(OpenLDAPConstants.ACCESS_MODE_SETTING, config.get(AbstractTokenUtil.ACCESSMODE));
        settingsUtils.changeSetting(OpenLDAPConstants.DOMAIN_SETTING, config.get(OpenLDAPConstants.CONFIG_DOMAIN));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_NAME_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_NAME_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_OBJECT_CLASS_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_OBJECT_CLASS));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_SEARCH_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_SEARCH_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_USER_MAPPING_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_USER_MAPPING_ATTRIBUTE));
        settingsUtils.changeSetting(OpenLDAPConstants.LOGIN_DOMAIN_SETTING, config.get(OpenLDAPConstants.CONFIG_LOGIN_DOMAIN));
        settingsUtils.changeSetting(OpenLDAPConstants.PORT_SETTING, config.get(OpenLDAPConstants.CONFIG_PORT));
        settingsUtils.changeSetting(OpenLDAPConstants.SERVER_SETTING, config.get(OpenLDAPConstants.CONFIG_SERVER));
        if(config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD) != null){
            settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_PASSWORD));
        }
        settingsUtils.changeSetting(OpenLDAPConstants.SERVICE_ACCOUNT_USERNAME_SETTING, config.get(OpenLDAPConstants.CONFIG_SERVICE_ACCOUNT_USERNAME));
        settingsUtils.changeSetting(OpenLDAPConstants.TLS_SETTING, config.get(OpenLDAPConstants.CONFIG_TLS));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_DISABLED_BIT_MASK_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_DISABLED_BIT_MASK));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_ENABLED_ATTRIBUTE));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_LOGIN_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_LOGIN_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_MEMBER_ATTRIBUTE));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_NAME_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_NAME_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_OBJECT_CLASS_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_OBJECT_CLASS));
        settingsUtils.changeSetting(OpenLDAPConstants.USER_SEARCH_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_USER_SEARCH_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.TIMEOUT_SETTING, config.get(OpenLDAPConstants.CONFIG_TIMEOUT));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_DN_FIELD_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_DN_FIELD));
        settingsUtils.changeSetting(OpenLDAPConstants.GROUP_MEMBER_USER_ATTRIBUTE_SETTING, config.get(OpenLDAPConstants.CONFIG_GROUP_MEMBER_USER_ATTRIBUTE));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(OpenLDAPConstants.CONFIG_SECURITY));
        if (config.get(OpenLDAPConstants.CONFIG_SECURITY) != null){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, OpenLDAPConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        String accessModeInConfig = (String)config.get(AbstractTokenUtil.ACCESSMODE);
        if (AbstractTokenUtil.isRestrictedAccess(accessModeInConfig) || AbstractTokenUtil.isRequiredAccess(accessModeInConfig)) {
            //validate the allowedIdentities
            @SuppressWarnings("unchecked")
            String ids = openLDAPIdentityProvider.validateIdentities((List<Map<String, String>>) config.get(OpenLDAPConstants.CONFIG_ALLOWED_IDENTITIES));
            settingsUtils.changeSetting(OpenLDAPConstants.ALLOWED_IDENTITIES_SETTING, ids);
        } else if (AbstractTokenUtil.isUnrestrictedAccess(accessModeInConfig)) {
            //clear out the allowedIdentities Set
            settingsUtils.changeSetting(OpenLDAPConstants.ALLOWED_IDENTITIES_SETTING, null);
        }
        return currentLdapConfig(config);
    }

    public String getName() {
        return OpenLDAPConstants.MANAGER;
    }

}
