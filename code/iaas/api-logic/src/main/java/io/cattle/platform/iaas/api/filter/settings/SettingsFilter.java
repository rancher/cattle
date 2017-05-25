package io.cattle.platform.iaas.api.filter.settings;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.ActiveSetting;
import io.cattle.platform.core.model.Setting;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;

public class SettingsFilter implements Predicate {

    Set<String> publicSettings;
    boolean all;
    boolean canListAll;
    private static final Set<String> SETTINGS_HIDDEN = new HashSet<>(Arrays.asList(new String[]{"api.auth.ldap.service.account.password",
            "api.auth.ldap.openldap.service.account.password", "api.auth.azure.admin.password", "api.auth.github.client.secret"}));
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
        boolean hidden = false;
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        if (!policy.getRoles().contains("service")) {
            hidden = true;
        }

        String name = null;

        if (object instanceof ActiveSetting) {
            name = ((ActiveSetting) object).getName();
            if (SETTINGS_HIDDEN.contains(name) && hidden) {
                ((ActiveSetting) object).setActiveValue(null);
                ((ActiveSetting) object).setValue(null);
            }
        } else if (object instanceof Setting) {
            name = ((Setting) object).getName();
            if (SETTINGS_HIDDEN.contains(name) && hidden) {
                ((Setting) object).setValue(null);
            }
        }

        if (name == null) {
            return false;
        }

        if (all && canListAll) {
            return true;
        }

        ActiveSetting setting = (ActiveSetting) object;
        if (SETTINGS_HIDDEN.contains(name) && hidden) {
            setting.setValue(null);
            setting.setActiveValue(null);
        }
        return publicSettings.contains(setting.getName());
    }

}
