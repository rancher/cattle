package io.cattle.platform.api.setting;

import com.netflix.config.DynamicStringListProperty;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SettingsFilter {

    public static final DynamicStringListProperty PUBLIC_SETTINGS = ArchaiusUtil.getList("settings.public");
    private static Set<String> PUBLIC = Collections.emptySet();

    static {
        PUBLIC_SETTINGS.addCallback(SettingsFilter::refresh);
    }

    boolean all;
    boolean canListAll;

    private static void refresh() {
        PUBLIC = new HashSet<>(PUBLIC_SETTINGS.get());
    }

    public SettingsFilter(ApiRequest apiRequest) {
        if (PUBLIC.size() == 0) {
            refresh();
        }

        String value = null;
        Map<String, String[]> params = apiRequest == null ? null : apiRequest.getRequestParams();

        if (params != null) {
            value = RequestUtils.getSingularStringValue("all", params);
        }

        this.all = value == null || !value.toString().equalsIgnoreCase("false");
        this.canListAll = "true".equals(ApiUtils.getPolicy().getOption(Policy.LIST_ALL_SETTINGS));
    }

    public boolean isAuthorized(String name) {
        if (name == null) {
            return false;
        }

        if (all && canListAll) {
            return true;
        }

        return PUBLIC.contains(name);
    }

    public static boolean isAuthorized(String name, ApiRequest apiRequest) {
        return new SettingsFilter(apiRequest).isAuthorized(name);
    }

}
