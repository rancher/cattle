package io.github.ibuildthecloud.dstack.archaius.util;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class ArchaiusUtil {

    public static DynamicLongProperty getLongProperty(String key) {
        return DynamicPropertyFactory.getInstance().getLongProperty(key, 0);
    }

    public static DynamicIntProperty getIntProperty(String key) {
        return DynamicPropertyFactory.getInstance().getIntProperty(key, 0);
    }

    public static DynamicBooleanProperty getBooleanProperty(String key) {
        return DynamicPropertyFactory.getInstance().getBooleanProperty(key, false);
    }

    public static DynamicDoubleProperty getDoubleProperty(String key) {
        return DynamicPropertyFactory.getInstance().getDoubleProperty(key, 0);
    }

    public static DynamicFloatProperty getFloatProperty(String key) {
        return DynamicPropertyFactory.getInstance().getFloatProperty(key, 0);
    }

    public static DynamicStringProperty getStringProperty(String key) {
        return DynamicPropertyFactory.getInstance().getStringProperty(key, null);
    }

}
