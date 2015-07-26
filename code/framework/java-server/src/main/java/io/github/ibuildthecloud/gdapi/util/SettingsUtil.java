package io.github.ibuildthecloud.gdapi.util;

public class SettingsUtil {

    public static String getSetting(Settings settings, String key, String defaultValue) {
        if (settings == null) {
            String value = System.getProperty(key);
            return value == null ? defaultValue : value;
        }

        String result = settings.getProperty(key);
        return result == null ? defaultValue : result;
    }

}
