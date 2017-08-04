package io.cattle.platform.api.setting;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Setting;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.util.Map;

public class SettingsFilter {

    boolean all;
    boolean canListAll;

    public SettingsFilter(ApiRequest apiRequest) {
        String value = null;
        Map<String, String[]> params = apiRequest == null ? null : apiRequest.getRequestParams();

        if (params != null) {
            value = RequestUtils.getSingularStringValue("all", params);
        }

        this.all = value == null || !value.toString().equalsIgnoreCase("false");
        this.canListAll = "true".equals(ApiUtils.getPolicy().getOption(Policy.LIST_ALL_SETTINGS));
    }

    public boolean isAuthorized(Setting setting) {
        if (setting == null) {
            return false;
        }
        return isAuthorized(setting.getName());
    }

    public boolean isAuthorized(String name) {
        if (name == null) {
            return false;
        }

        if (all && canListAll) {
            return true;
        }

        return SettingManager.PUBLIC.contains(name);
    }

    public static boolean isAuthorized(String name, ApiRequest apiRequest) {
        return new SettingsFilter(apiRequest).isAuthorized(name);
    }

}
