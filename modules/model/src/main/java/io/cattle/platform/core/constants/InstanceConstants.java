
package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceConstants {

    public static final String ACTION_SOURCE_USER = "user";
    public static final String ACTION_SOURCE_EXTERNAL = "external";

    public static final String TYPE = "instance";
    public static final String TYPE_CONTAINER = "container";

    public static final String FIELD_AGENT_INSTANCE = "agentInstance";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_IMAGE_UUID = "imageUuid";
    public static final String FIELD_INSTANCE_LINKS = "instanceLinks";
    public static final String FIELD_INSTANCE_TRIGGERED_STOP = "instanceTriggeredStop";
    public static final String FIELD_NETWORK_IDS = "networkIds";
    public static final String FIELD_PORTS = "ports";
    public static final String FIELD_PORT_BINDINGS = "portBindings";
    public static final String FIELD_MANAGED_IP = "managedIp";
    public static final String FIELD_PRIMARY_IP_ADDRESS = "primaryIpAddress";
    public static final String FIELD_PRIMARY_NETWORK_ID = "primaryNetworkId";
    public static final String FIELD_PRIMARY_MAC_ADDRESSS = "primaryMacAddress";
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
    public static final String FIELD_DEPLOYMENT_UNIT_ID = "deploymentUnitId";
    public static final String FIELD_DATA_VOLUME_MOUNTS = "dataVolumeMounts";
    public static final String FIELD_DATA_VOLUMES = "dataVolumes";
    public static final String FIELD_VOLUME_DRIVER = "volumeDriver";
    public static final String FIELD_SYSTEM_CONTAINER = "systemContainer";
    public static final String FIELD_DISKS = "disks";
    public static final String FIELD_ALLOCATED_IP_ADDRESS = "allocatedIpAddress";
    public static final String FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID = "serviceIndexId";
    public static final String FIELD_SERVICE_INSTANCE_SERVICE_INDEX = "serviceIndex";
    public static final String FIELD_METADATA = "metadata";
//    public static final String FIELD_HOST_ID = "hostId";
    public static final String FIELD_LOG_CONFIG = "logConfig";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_SECRETS = "secrets";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_MEMORY_RESERVATION = "memoryReservation";
    public static final String FIELD_MOUNTS = "mounts";
    public static final String FIELD_HEALTHCHECK_STATES = "healthcheckStates";
    public static final String FIELD_REVISION_CONFIG = "config";
    public static final String FIELD_SERVICE_ID = "serviceId";
    public static final String FIELD_STACK_ID = "stackId";
    public static final String FIELD_SIDEKICK_TO = "sidekickTo";
    public static final String FIELD_STOP_SOURCE = "stopSource";
    public static final String FIELD_EXIT_CODE = "exitCode";
    public static final String FIELD_START_RETRY_COUNT = "startRetryCount";
    public static final String FIELD_REVISION_ID = "revisionId";
    public static final String FIELD_PREVIOUS_REVISION_ID = "previousRevisionId";
    public static final String FIELD_IMAGE_PRE_PULL = "prePullOnUpgrade";
    public static final String FIELD_SHOULD_RESTART = "shouldRestart";
    public static final String FIELD_LAST_START = "lastStart";
    public static final String FIELD_LAUNCH_CONFIG_NAME = "launchConfigName";
    public static final String FIELD_LB_RULES_ON_REMOVE = "lbRulesOnRemove";

    public static final String PULL_NONE = "none";
    public static final String PULL_ALL = "all";
    public static final String PULL_EXISTING = "existing";

    public static final String PROCESS_DATA_NO_OP = "containerNoOpEvent";

    public static final String PROCESS_DATA_ERROR = "errorState";

    public static final String REMOVE_OPTION = "remove";

    public static final String PROCESS_ALLOCATE = "instance.allocate";
    public static final String PROCESS_DEALLOCATE = "instance.deallocate";
    public static final String PROCESS_CREATE = "instance.create";
    public static final String PROCESS_START = "instance.start";
    public static final String PROCESS_STOP = "instance.stop";
    public static final String PROCESS_RESTART = "instance.restart";
    public static final String PROCESS_REMOVE = "instance.remove";
    public static final String PROCESS_ERROR = "instance.error";

    public static final String ACTION_CONVERT_TO_SERVICE = "instance.converttoservice";

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

    public static final String VOLUME_CLEANUP_STRATEGY_NONE = "none";
    public static final String VOLUME_CLEANUP_STRATEGY_UNNAMED = "unnamed";
    public static final String VOLUME_CLEANUP_STRATEGY_ALL = "all";
    public static final Set<String> VOLUME_REMOVE_STRATEGIES = CollectionUtils.set(
            VOLUME_CLEANUP_STRATEGY_NONE,
            VOLUME_CLEANUP_STRATEGY_UNNAMED,
            VOLUME_CLEANUP_STRATEGY_ALL);

    public static boolean isSystem(Instance instance) {
        return instance.getSystem() || isRancherAgent(instance);
    }

    public static boolean isRancherAgent(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        return ("rancher-agent".equals(labels.get("io.rancher.container.system")) &&
                "rancher-agent".equals(instance.getName()));

    }

    public static List<Long> getInstanceDependencies(Instance instance) {
        List<Long> instanceIds = new ArrayList<>();
        Long networkFromContainerId = instance.getNetworkContainerId();
        if (networkFromContainerId != null) {
            instanceIds.add(networkFromContainerId);
        }
        Long sidekickTo = DataAccessor.fieldLong(instance, FIELD_SIDEKICK_TO);
        if (sidekickTo != null) {
            instanceIds.add(sidekickTo);
        }
        instanceIds.addAll(DataAccessor.fieldLongList(instance, "dataVolumesFrom"));
        return instanceIds;
    }

}
