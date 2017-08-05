package io.cattle.platform.api.setting;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.core.model.Setting;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingManager implements ResourceManager {

    SettingDao settingDao;

    public SettingManager(SettingDao settingDao) {
        this.settingDao = settingDao;
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        if (!SettingsFilter.isAuthorized(id, ApiContext.getContext().getApiRequest())) {
            return false;
        }

        return new SettingResource(id);
    }

    @Override
    public List<?> list(String type, ApiRequest request) {
        SettingsFilter filter = new SettingsFilter(request);
        List<SettingResource> result = new ArrayList<>();

        ArchaiusUtil.getConfiguration().getKeys().forEachRemaining((name) -> {
            if (!filter.isAuthorized(name)) {
                return;
            }

            result.add(new SettingResource(name));
        });

        return result;
    }

    @Override
    public List<?> list(String type, Map<Object, Object> criteria, ListOptions options) {
        return list(type, ApiContext.getContext().getApiRequest());
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Setting setting = request.proxyRequestObject(Setting.class);
        if (setting.getName() == null || setting.getValue() == null) {
            return null;
        }

        settingDao.setValue(setting.getName(), setting.getValue());
        ArchaiusUtil.refresh();
        return getById(type, setting.getName(), null);
    }

    @Override
    public Object update(String type, String id, ApiRequest request) {
        String value = request.proxyRequestObject(Setting.class).getValue();
        if (value == null) {
            return delete(type, id, request);
        }

        settingDao.setValue(id, value);
        ArchaiusUtil.refresh();
        return getById(type, id, null);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request) {
        if (!SettingsFilter.isAuthorized(id, request)) {
            return null;
        }

        settingDao.deleteSetting(id);
        ArchaiusUtil.refresh();
        return getById(type, id, null);
    }

}
