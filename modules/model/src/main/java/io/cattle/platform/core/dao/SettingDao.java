package io.cattle.platform.core.dao;

public interface SettingDao {

    void setValue(String name, Object value);

    void deleteSetting(String name);

}
