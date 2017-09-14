package io.cattle.platform.engine.process.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;

public class ProcessEngineUtils {

    private static final DynamicBooleanProperty PROVISIONING_ENABLED = ArchaiusUtil.getBoolean("provisioning.enabled");

    public static boolean enabled() {
        return PROVISIONING_ENABLED.get();
    }

}