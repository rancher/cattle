package io.github.ibuildthecloud.dstack.api.utils;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.util.Settings;

public class ApiSettings implements Settings {

    @Override
    public String getProperty(String key) {
        return ArchaiusUtil.getString(key).get();
    }

}
