package io.cattle.platform.core.constants;

public class HostConstants {

    public static final String TYPE = "host";
    public static final String FIELD_PHYSICAL_HOST_ID = "physicalHostId";

    /* This is not a real field in the API but passed as a field on agent ping */
    public static final String FIELD_CREATE_LABELS = "createLabels";

    public static final String FIELD_REPORTED_UUID = "reportedUuid";
    public static final String FIELD_PHYSICAL_HOST_UUID = "physicalHostUuid";
    public static final String FIELD_HOST_UUID = "hostUuid";
    public static final String FIELD_INFO = "info";
    public static final String FIELD_API_PROXY = "apiProxy";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_IP_ADDRESS = "agentIpAddress";
    public static final String FIELD_INSTANCE_IDS = "instanceIds";
    public static final String FIELD_AGENT_ID = "agentId";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_MILLI_CPU = "milliCpu";
    public static final String FIELD_LOCAL_STORAGE_MB = "localStorageMb";

    public static final String PROCESS_REMOVE = "host.remove";
    public static final String PROCESS_CREATE = "host.create";
    public static final String PROCESS_UPDATE = "host.update";
    public static final String PROCESS_ACTIVATE = "host.activate";
    public static final String PROCESS_PROVISION = "host.provision";

    public static final String STATE_PROVISIONING = "provisioning";

}