package io.cattle.platform.core.constants;

public class VolumeConstants {

    public static final String REMOVE_OPTION = "remove";

    public static final String FILE_PREFIX = "file";

    public static final String URI_FORMAT = "%s://%s";

    public static final String FIELD_VOLUME_DRIVER = "driver";

    public static final String FIELD_VOLUME_DRIVER_OPTS = "driverOpts";

    public static final String FIELD_URI = "uri";

    public static final String FIELD_DEVICE_NUM = "deviceNumber";

    public static final String LOCAL_DRIVER = "local";

    public static final String PROCESS_CREATED = "volume.create";

    public static final String PROCESS_ACTIVATE = "volume.activate";

    public static final String PROCESS_DEACTIVATE = "volume.deactivate";

    public static final String PROCESS_REMOVE = "volume.remove";

    public static final String ACCESS_MODE_SINGLE_HOST_RW = "singleHostRW";

    public static final String ACCESS_MODE_SINGLE_INSTANCE_RW = "singleInstanceRW";

    public static final String CAPABILITY_SNAPSHOT = "snapshot";

    public static final String ACTION_SNAPSHOT = "snapshot";

    public static final String ACTION_REVERT = "reverttosnapshot";

    public static final String ACTION_RESTORE = "restorefrombackup";

    public static final String PROCESS_SNAPSHOT = "volume." + ACTION_SNAPSHOT;

    public static final String PROCESS_REVERT = "volume." + ACTION_REVERT;

    public static final String PROCESS_RESTORE_FROM_BACKUP = "volume." + ACTION_RESTORE;

    public static final String DRIVER_OPT_BASE_IMAGE = "base-image";
}
