package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VolumeConstants {

    public static final String TYPE = "volume";

    public static final String ACCESS_MODE_MULTI_HOST_RW = "multiHostRW";
    public static final String ACCESS_MODE_SINGLE_HOST_RW = "singleHostRW";
    public static final String ACCESS_MODE_SINGLE_INSTANCE_RW = "singleInstanceRW";
    public static final String DEFAULT_ACCESS_MODE = ACCESS_MODE_MULTI_HOST_RW;
    public static final String DRIVER_OPT_BASE_IMAGE = "base-image";
    public static final String FIELD_DOCKER_IS_NATIVE = "isNative";
    public static final String FIELD_LAST_ALLOCATED_HOST_ID = "lastAllocatedHostID";
    public static final String FIELD_STORAGE_DRIVER_ID = "storageDriverId";
    public static final String FIELD_VOLUME_DRIVER = "driver";
    public static final String FIELD_VOLUME_DRIVER_OPTS = "driverOpts";
    public static final String FILE_PREFIX = "file";
    public static final String LOCAL_DRIVER = "local";
    public static final String PROCESS_ACTIVATE = "volume.activate";
    public static final String PROCESS_CREATE = "volume.create";
    public static final String STATE_DETACHED = "detached";

    public static final String SECRETS_PATH = "/run/secrets";
    public static final String SECRETS_OPT_KEY = "io.rancher.secrets.token";
    public static final String EC2_AZ = "ec2_az";
    public static final String HOST_ZONE_LABEL_KEY = "io.rancher.host.zone";

    public static final Set<String> VALID_ACCESS_MODES = new HashSet<>(Arrays.asList(
            ACCESS_MODE_MULTI_HOST_RW,
            ACCESS_MODE_SINGLE_HOST_RW,
            ACCESS_MODE_SINGLE_INSTANCE_RW));

}
