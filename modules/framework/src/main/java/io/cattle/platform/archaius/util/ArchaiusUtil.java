package io.cattle.platform.archaius.util;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.polling.RefreshableFixedDelayPollingScheduler;
import io.cattle.platform.archaius.sources.FixedConfigurationSource;
import io.cattle.platform.archaius.sources.NamedConfigurationSource;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

public class ArchaiusUtil {

    private static RefreshableFixedDelayPollingScheduler scheduler;

    public static DynamicLongProperty getLong(String key) {
        return DynamicPropertyFactory.getInstance().getLongProperty(key, 0);
    }

    public static DynamicIntProperty getInt(String key) {
        return DynamicPropertyFactory.getInstance().getIntProperty(key, 0);
    }

    public static DynamicBooleanProperty getBoolean(String key) {
        return DynamicPropertyFactory.getInstance().getBooleanProperty(key, false);
    }

    public static DynamicDoubleProperty getDouble(String key) {
        return DynamicPropertyFactory.getInstance().getDoubleProperty(key, 0);
    }

    public static DynamicFloatProperty getFloat(String key) {
        return DynamicPropertyFactory.getInstance().getFloatProperty(key, 0);
    }

    public static DynamicStringProperty getString(String key) {
        return DynamicPropertyFactory.getInstance().getStringProperty(key, null);
    }

    public static Configuration getConfiguration() {
        return (Configuration) DynamicPropertyFactory.getBackingConfigurationSource();
    }

    /**
     * Please only use this as a static variable. Calling getList(..).get()
     * repeatedly will probably cause a memory leak
     *
     * @param key
     * @return
     */
    public static DynamicStringListProperty getList(String key) {
        return new DynamicStringListProperty(key, (String) null);
    }

    public static String getStringValue(String name) {
        Configuration config = getConfiguration();
        if (config == null) {
            return null;
        }
        return config.getString(name);
    }

    public static boolean isFixed(String name) {
        return getSourceConfiguration(name) instanceof FixedConfigurationSource;
    }

    public static String getSource(String name) {
        return toSourceName(getSourceConfiguration(name));
    }

    private static Configuration getSourceConfiguration(String name) {
        Configuration source = getConfiguration();
        if (source instanceof ConcurrentCompositeConfiguration) {
            source = ((ConcurrentCompositeConfiguration) source).getSource(name);
        } else if (source instanceof CompositeConfiguration) {
            source = ((CompositeConfiguration) source).getSource(name);
        }
        return source;
    }

    public static String toSourceName(Configuration config) {
        if (config instanceof NamedConfigurationSource) {
            return ((NamedConfigurationSource) config).getSourceName();
        }

        if (config instanceof DynamicConfiguration) {
            return ((DynamicConfiguration) config).getSource().getClass().getName();
        }

        return config == null ? null : config.getClass().getName();
    }

    public static void setScheduler(RefreshableFixedDelayPollingScheduler scheduler) {
        ArchaiusUtil.scheduler = scheduler;
    }

    public static void refresh() {
        if (scheduler != null) {
            scheduler.refresh();
        }
    }
}
