package io.cattle.platform.core.constants;

public class HostConstants {

    public static final String TYPE = "host";


    public static final String FIELD_AGENT_ID = "agentId";
    /* This is not a real field in the API but passed as a field on agent ping */
    public static final String FIELD_CREATE_LABELS = "createLabels";
    public static final String FIELD_DRIVER = "driver";
    public static final String FIELD_EXTERNAL_ID = "externalId";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_HOST_TEMPLATE_ID = "hostTemplateId";
    public static final String FIELD_HOST_UUID = "hostUuid";
    public static final String FIELD_INFO = "info";
    public static final String FIELD_IMPORTED = "imported";
    public static final String FIELD_INSTANCE_IDS = "instanceIds";
    public static final String FIELD_IP_ADDRESS = "agentIpAddress";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_LOCAL_STORAGE_MB = "localStorageMb";
    public static final String FIELD_MACHINE_SERVICE_REGISTRATION_UUID = "machineServiceRegistrationUuid";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_MILLI_CPU = "milliCpu";
    public static final String FIELD_NODE_NAME = "nodeName";
    public static final String FIELD_PUBLIC_ENDPOINTS = "publicEndpoints";

    public static final String PROCESS_ACTIVATE = "host.activate";
    public static final String PROCESS_PROVISION = "host.provision";
    public static final String PROCESS_EVACUATE = "host.evacuate";

    public static final String STATE_PROVISIONING = "provisioning";

    public static final String CONFIG_FIELD_SUFFIX = "Config";
    public static final String CONFIG_LINK = "config";
    public static final String STORAGE_POOLS_LINK = "storagePools";
    public static final String EXTRACTED_CONFIG_FIELD = "extractedConfig";

}