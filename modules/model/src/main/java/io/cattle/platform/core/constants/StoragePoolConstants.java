package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StoragePoolConstants {

    public static final String TYPE = "storagePool";

    public static final String KIND_REGISTRY = "registry";

    public static final String SERVER_ADDRESS = "serverAddress";

    public static final String FIELD_BLOCK_DEVICE_PATH = "blockDevicePath";

    public static final String FIELD_VOLUME_CAPABILITIES = "volumeCapabilities";

    public static final String FIELD_HOST_IDS = "hostIds";

    public static final String FIELD_VOLUME_IDS = "volumeIds";

    public static final String FIELD_REPORTED_UUID = "reportedUuid";

    public static final Set<String> UNMANGED_STORAGE_POOLS = new HashSet<>(Arrays.asList(new String[]{"docker", "sim"}));

}
