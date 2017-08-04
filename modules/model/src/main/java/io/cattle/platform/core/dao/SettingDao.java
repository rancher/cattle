package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Setting;

public interface SettingDao {

    Setting getSetting(String name);

    void setValue(String name, Object value);

    void deleteSetting(String name);

}
