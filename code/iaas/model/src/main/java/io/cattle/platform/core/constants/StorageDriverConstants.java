package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StorageDriverConstants {

    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_VOLUME_ACCESS_MODE = "volumeAccessMode";
    public static final String FIELD_VOLUME_CAPABILITES = StoragePoolConstants.FIELD_VOLUME_CAPABILITIES;
    public static final String FIELD_BLOCK_DEVICE_PATH = StoragePoolConstants.FIELD_BLOCK_DEVICE_PATH;

    public static final String SCOPE_LOCAL = "local";
    public static final String SCOPE_ENVIRONMENT = "environment";
    public static final String SCOPE_CUSTOM = "custom";

    public static final String DEFAULT_SCOPE = SCOPE_ENVIRONMENT;
    public static final Set<String> VALID_SCOPES = new HashSet<>(Arrays.asList(
            SCOPE_CUSTOM,
            SCOPE_LOCAL,
            SCOPE_ENVIRONMENT
            ));

    public static final String CAPABILITY_SCHEDULE_SIZE = "schedule.size";
    public static final String CAPABILITY_SECRETS = "secrets";
}
