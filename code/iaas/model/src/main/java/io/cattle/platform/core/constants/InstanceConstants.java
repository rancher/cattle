package io.cattle.platform.core.constants;

public class InstanceConstants {

    public enum SystemContainer {
        NetworkAgent,
        LoadBalancerAgent,
        ClusterAgent;       // swarm
    }

    public static final String TYPE = "instance";
    public static final String TYPE_CONTAINER = "container";

    public static final String FIELD_AGENT_INSTANCE = "agentInstance";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_CREDENTIAL_IDS = "credentialIds";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_IMAGE_UUID = "imageUuid";
    public static final String FIELD_IMAGE_ID = "imageId";
    public static final String FIELD_INSTANCE_LINKS = "instanceLinks";
    public static final String FIELD_INSTANCE_TRIGGERED_STOP = "instanceTriggeredStop";
    public static final String FIELD_PUBLIC_IP_ADDRESS_POOL_ID = "publicIpAddressPoolId";
    public static final String FIELD_NETWORK_IDS = "networkIds";
    public static final String FIELD_PORTS = "ports";
    public static final String FIELD_PRIMARY_IP_ADDRESS = "primaryIpAddress";
    public static final String FIELD_PRIMARY_ASSOCIATED_IP_ADDRESS = "primaryAssociatedIpAddress";
    public static final String FIELD_PRIVILEGED = "privileged";
    public static final String FIELD_REQUESTED_HOST_ID = "requestedHostId";
    public static final String FIELD_REQUESTED_IP_ADDRESS = "requestedIpAddress";
    public static final String FIELD_VALID_HOST_IDS = "validHostIds";
    public static final String FIELD_SUBNET_IDS = "subnetIds";
    public static final String FIELD_START_ON_CREATE = "startOnCreate";
    public static final String FIELD_VCPU = "vcpu";
    public static final String FIELD_VNET_IDS = "vnetIds";
    public static final String FIELD_VOLUME_OFFERING_IDS = "volumeOfferingIds";
    public static final String FIELD_COMPUTE_TRIES = "computeTries";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_HEALTH_CHECK = "healthCheck";
    public static final String FIELD_EXPOSE = "expose";

    public static final String PROCESS_DATA_NO_OP = "containerNoOpEvent";

    public static final String REMOVE_OPTION = "remove";
    public static final String DEALLOCATE_OPTION = "deallocateFromHost";

    public static final String PROCESS_CREATE = "instance.create";
    public static final String PROCESS_START = "instance.start";
    public static final String PROCESS_STOP = "instance.stop";
    public static final String PROCESS_RESTART = "instance.restart";
    public static final String PROCESS_REMOVE = "instance.remove";

    public static final String KIND_CONTAINER = "container";
    public static final String KIND_VIRTUAL_MACHINE = "virtualMachine";

    public static final String STATE_CREATING = "creating";
    public static final String STATE_CREATED = "created";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_STOPPING = "stopping";
    public static final String STATE_STARTING = "starting";
    public static final String STATE_RESTARTING = "restarting";

    public static final String ON_STOP_STOP = "stop";
    public static final String ON_STOP_RESTART = "restart";
    public static final String ON_STOP_REMOVE = "remove";

    public static final String EVENT_INSTANCE_FORCE_STOP = "compute.instance.force.stop";
}
