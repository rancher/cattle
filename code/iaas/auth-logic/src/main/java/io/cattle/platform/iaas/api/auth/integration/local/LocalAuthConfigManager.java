package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.PasswordDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LocalAuthConfigManager extends AbstractNoOpResourceManager {

    @Inject
    PasswordDao passwordDao;

    @Inject
    SettingsUtils settingsUtils;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {LocalAuthConfig.class};
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equalsIgnoreCase(LocalAuthConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = CollectionUtils.toMap(request.getRequestObject());

        String username = (String) config.get("username");
        String name = (String) config.get("name");
        String password = (String) config.get("password");
        String accessMode = (String) config.get("accessMode");
        Boolean enabled = (Boolean) config.get("enabled");

        if (enabled == null) {
            settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, false);
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
            return new LocalAuthConfig("", "", "", accessMode, false);
        } else {
            settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, enabled);
            if (StringUtils.isNotBlank(username)) {
                LocalAuthPasswordValidator.validatePassword(password, jsonMapper);
                passwordDao.verifyUsernamePassword(username, password, name);
            }
        }

        settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, LocalAuthConstants.CONFIG);
        settingsUtils.changeSetting(LocalAuthConstants.ACCESS_MODE_SETTING, accessMode);

        return new LocalAuthConfig(username, name, password, accessMode, enabled);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new LocalAuthConfig("", "", "", LocalAuthConstants.ACCESS_MODE.get(), SecurityConstants.SECURITY.get());
    }
}
