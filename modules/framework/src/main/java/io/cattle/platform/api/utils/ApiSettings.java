package io.cattle.platform.api.utils;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.util.Settings;

public class ApiSettings implements Settings {

    @Override
    public String getProperty(String key) {
        return ArchaiusUtil.getString(key).get();
    }

}
