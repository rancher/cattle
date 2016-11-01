package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InstanceConstants {

    public static final String SYSTEM_CONTAINER_NETWORK_AGENT = "NetworkAgent";
    public static final String SYSTEM_CONTAINER_LB_AGENT = "LoadBalancerAgent";
    public static final String SYSTEM_CONTAINER_BALANCER_AGENT = "BalancerAgent";
    public static final String SYSTEM_CONTAINER_CLUSTER_AGENT = "ClusterAgent";

    public static final String TYPE = "instance";
    public static final String TYPE_CONTAINER = "container";

    public static final String FIELD_AGENT_INSTANCE = "agentInstance";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_CREDENTIAL_IDS = "credentialIds";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_IMAGE_UUID = "imageUuid";
    public static final String FIELD_INSTANCE_LINKS = "instanceLinks";
    public static final String FIELD_INSTANCE_TRIGGERED_STOP = "instanceTriggeredStop";
    public static final String FIELD_NETWORK_IDS = "networkIds";
    public static final String FIELD_PORTS = "ports";
    public static final String FIELD_PRIMARY_IP_ADDRESS = "primaryIpAddress";
    public static final String FIELD_PRIVILEGED = "privileged";
    public static final String FIELD_REQUESTED_HOST_ID = "requestedHostId";
    public static final String FIELD_REQUESTED_IP_ADDRESS = "requestedIpAddress";
    public static final String FIELD_START_ON_CREATE = "startOnCreate";
    public static final String FIELD_VCPU = "vcpu";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_HEALTH_CHECK = "healthCheck";
    public static final String FIELD_EXPOSE = "expose";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_CREATE_INDEX = "createIndex";
    public static final String FIELD_DEPLOYMENT_UNIT_UUID = "deploymentUnitUuid";
    public static final String FIELD_DATA_VOLUME_MOUNTS = "dataVolumeMounts";
    public static final String FIELD_DATA_VOLUMES = "dataVolumes";
    public static final String FIELD_VOLUME_DRIVER = "volumeDriver";
    public static final String FIELD_SYSTEM_CONTAINER = "systemContainer";
    public static final String FIELD_DISKS = "disks";
    public static final String FIELD_HEALTH_UPDATED = "healthUpdated";
    public static final String FIELD_ALLOCATED_IP_ADDRESS = "allocatedIpAddress";
    public static final String FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID = "serviceIndexId";
    public static final String FIELD_SERVICE_INSTANCE_SERVICE_INDEX = "serviceIndex";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_HOST_ID = "hostId";
    public static final String FIELD_DNS_INTERNAL = "dnsInternal";
    public static final String FIELD_DNS_SEARCH_INTERNAL = "dnsSearchInternal";
    public static final String FIELD_LOG_CONFIG = "logConfig";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_MEMORY_RESERVATION = "memoryReservation";

    public static final String PROCESS_DATA_NO_OP = "containerNoOpEvent";

    public static final String PROCESS_DATA_ERROR = "errorState";

    public static final String REMOVE_OPTION = "remove";

    public static final String PROCESS_CREATE = "instance.create";
    public static final String PROCESS_START = "instance.start";
    public static final String PROCESS_STOP = "instance.stop";
    public static final String PROCESS_RESTART = "instance.restart";
    public static final String PROCESS_REMOVE = "instance.remove";
    public static final String PROCESS_RESTORE = "instance.restore";
    public static final String PROCESS_PURGE = "instance.purge";
    public static final String PROCESS_ERROR = "instance.error";

    public static final String KIND_CONTAINER = "container";
    public static final String KIND_VIRTUAL_MACHINE = "virtualMachine";

    public static final String STATE_CREATING = "creating";
    public static final String STATE_CREATED = "created";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_STOPPING = "stopping";
    public static final String STATE_STARTING = "starting";
    public static final String STATE_RESTARTING = "restarting";
    public static final String STATE_ERRORING = "erroring";
    public static final String STATE_ERROR = "error";

    public static final String ON_STOP_REMOVE = "remove";

    public static final String EVENT_INSTANCE_FORCE_STOP = "compute.instance.force.stop";

    public static final Set<String> CONTAINER_LIKE = new HashSet<>(Arrays.asList(KIND_CONTAINER, KIND_VIRTUAL_MACHINE));

    public static final String VOLUME_CLEANUP_STRATEGY_NONE = "none";
    public static final String VOLUME_CLEANUP_STRATEGY_UNNAMED = "unnamed";
    public static final String VOLUME_CLEANUP_STRATEGY_ALL = "all";
    public static final Set<String> VOLUME_REMOVE_STRATEGIES = new HashSet<>(Arrays.asList(VOLUME_CLEANUP_STRATEGY_NONE, VOLUME_CLEANUP_STRATEGY_UNNAMED,
            VOLUME_CLEANUP_STRATEGY_ALL));
}
