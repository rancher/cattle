package io.cattle.platform.engine.process.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import com.netflix.config.DynamicBooleanProperty;

public class ProcessEngineUtils {

    private static final DynamicBooleanProperty HA_CONTAINER = ArchaiusUtil.getBoolean("ha.container");
    public static final DynamicBooleanProperty HA_ENABLED = ArchaiusUtil.getBoolean("ha.enabled");
    private static final DynamicBooleanProperty PROVISIONING_ENABLED = ArchaiusUtil.getBoolean("provisioning.enabled");

    public static boolean enabled() {
        if (!PROVISIONING_ENABLED.get()) {
            return false;
        }

        return HA_ENABLED.get() == HA_CONTAINER.get();
    }

}