package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.PasswordDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class LocalAuthConfigManager extends AbstractNoOpResourceManager {

    PasswordDao passwordDao;
    SettingDao settingsUtils;
    JsonMapper jsonMapper;

    public LocalAuthConfigManager(PasswordDao passwordDao, SettingDao settingsUtils, JsonMapper jsonMapper) {
        super();
        this.passwordDao = passwordDao;
        this.settingsUtils = settingsUtils;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Object create(String type, ApiRequest request) {
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
            settingsUtils.setValue(SecurityConstants.SECURITY_SETTING, Boolean.toString(false));
            settingsUtils.setValue(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
            return new LocalAuthConfig("", "", "", accessMode, false);
        } else {
            settingsUtils.setValue(SecurityConstants.SECURITY_SETTING, Boolean.toString(enabled));
            if (StringUtils.isNotBlank(username)) {
                LocalAuthPasswordValidator.validatePassword(password, jsonMapper);
                passwordDao.verifyUsernamePassword(username, password, name);
            }
        }

        settingsUtils.setValue(SecurityConstants.AUTH_PROVIDER_SETTING, LocalAuthConstants.CONFIG);
        settingsUtils.setValue(LocalAuthConstants.ACCESS_MODE_SETTING, accessMode);

        return new LocalAuthConfig(username, name, password, accessMode, enabled);
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new LocalAuthConfig("", "", "", LocalAuthConstants.ACCESS_MODE.get(), SecurityConstants.SECURITY.get());
    }
}
