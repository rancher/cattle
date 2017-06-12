package io.cattle.platform.api.settings.manager;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.settings.model.ActiveSetting;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Setting;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;

public class SettingsFilter implements Predicate {

    Set<String> publicSettings;
    boolean all;
    boolean canListAll;

    public SettingsFilter(List<String> publicSettings, ApiRequest apiRequest) {
        String value = null;
        Map<String, Object> params = apiRequest == null ? null : apiRequest.getRequestParams();

        if (params != null) {
            value = RequestUtils.getSingularStringValue("all", params);
        }

        this.publicSettings = new HashSet<>(publicSettings);
        this.all = value == null || !value.toString().equalsIgnoreCase("false");
        this.canListAll = "true".equals(ApiUtils.getPolicy().getOption(Policy.LIST_ALL_SETTINGS));
    }

    @Override
    public boolean evaluate(Object object) {
        String name = null;
        if (object instanceof ActiveSetting) {
            name = ((ActiveSetting) object).getName();
        } else if (object instanceof Setting) {
            name = ((Setting) object).getName();
        }

        if (name == null) {
            return false;
        }

        if (all && canListAll) {
            return true;
        }

        ActiveSetting setting = (ActiveSetting) object;
        return publicSettings.contains(setting.getName());
    }

}
