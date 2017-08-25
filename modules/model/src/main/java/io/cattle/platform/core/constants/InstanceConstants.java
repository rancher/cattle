
package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

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
    public static final String FIELD_BLKIO_DEVICE_OPTIONS = "blkioDeviceOptions";
    public static final String FIELD_BLKIO_WEIGHT = "blkioWeight";
    public static final String FIELD_CAP_ADD = "capAdd";
    public static final String FIELD_CAP_DROP = "capDrop";
    public static final String FIELD_CGROUP_PARENT = "cgroupParent";
    public static final String FIELD_COMMAND = "command";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_CPU_PERIOD = "cpuPeriod";
    public static final String FIELD_CPU_QUOTA = "cpuQuota";
    public static final String FIELD_CPU_SET = "cpuSetCpu";
    public static final String FIELD_CPUSET_MEMS = "cpuSetMems";
    public static final String FIELD_CPU_SHARES = "cpuShares";
    public static final String FIELD_CREATE_INDEX = "createIndex";
    public static final String FIELD_CREATE_ONLY = "createOnly";
    public static final String FIELD_DATA_VOLUME_MOUNTS = "dataVolumeMounts";
    public static final String FIELD_DATA_VOLUMES = "dataVolumes";
    public static final String FIELD_DEPENDS_ON = "dependsOn";
    public static final String FIELD_DEPLOYMENT_UNIT_ID = "deploymentUnitId";
    public static final String FIELD_DEPLOYMENT_UNIT_UUID = "deploymentUnitUuid";
    public static final String FIELD_DEVICES = "devices";
    public static final String FIELD_DISKS = "disks";
    public static final String FIELD_DNS = "dns";
    public static final String FIELD_DNS_OPT = "dnsOpt";
    public static final String FIELD_DNS_SEARCH = "dnsSearch";
    public static final String FIELD_DOCKER_INSPECT = "dockerInspect";
    public static final String FIELD_DOCKER_IP = "dockerIp";
    public static final String FIELD_DOMAIN_NAME = "domainName";
    public static final String FIELD_ENTRY_POINT = "entryPoint";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_EXIT_CODE = "exitCode";
    public static final String FIELD_EXPOSE = "expose";
    public static final String FIELD_EXTERNAL_COMPUTE_AGENT = "externalComputeAgent";
    public static final String FIELD_EXTRA_HOSTS = "extraHosts";
    public static final String FIELD_GROUP_ADD = "groupAdd";
    public static final String FIELD_HEALTH_CHECK = "healthCheck";
    public static final String FIELD_HEALTHCHECK_STATES = "healthcheckStates";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_IMAGE_PRE_PULL = "prePullOnUpgrade";
    @Deprecated
    public static final String FIELD_IMAGE_UUID = "imageUuid";
    public static final String FIELD_IMAGE = "image";
    public static final String FIELD_IPC_CONTAINER_ID = "ipcContainerId";
    public static final String FIELD_IPC_MODE = "ipcMode";
    public static final String FIELD_ISOLATION = "isolation";
    public static final String FIELD_KERNEL_MEMORY = "kernelMemory";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_LAST_START = "lastStart";
    public static final String FIELD_LAUNCH_CONFIG_NAME = "launchConfigName";
    public static final String FIELD_LINKS = "instanceLinks";
    public static final String FIELD_LB_RULES_ON_REMOVE = "lbRulesOnRemove";
    public static final String FIELD_LOG_CONFIG = "logConfig";
    public static final String FIELD_LXC_CONF = "lxcConf";
    public static final String FIELD_MANAGED_IP = "managedIp";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_MEMORY_RESERVATION = "memoryReservation";
    public static final String FIELD_MEMORY_SWAP = "memorySwap";
    public static final String FIELD_MEMORY_SWAPPINESS = "memorySwappiness";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_MOUNTS = "mounts";
    public static final String FIELD_NETWORK_CONTAINER_ID = "networkContainerId";
    public static final String FIELD_NETWORK_IDS = "networkIds";
    public static final String FIELD_NETWORK_MODE = "networkMode";
    public static final String FIELD_OOMKILL_DISABLE = "oomKillDisable";
    public static final String FIELD_OOM_SCORE_ADJ = "oomScoreAdj";
    public static final String FIELD_PID_CONTAINER_ID = "pidContainerId";
    public static final String FIELD_PID_MODE = "pidMode";
    public static final String FIELD_PORT_BINDINGS = "publicEndpoints";
    public static final String FIELD_PORTS = "ports";
    public static final String FIELD_PREVIOUS_REVISION_ID = "previousRevisionId";
    public static final String FIELD_PRIMARY_IP_ADDRESS = "primaryIpAddress";
    public static final String FIELD_PRIMARY_MAC_ADDRESSS = "primaryMacAddress";
    public static final String FIELD_PRIMARY_NETWORK_ID = "primaryNetworkId";
    public static final String FIELD_PRIVILEGED = "privileged";
    public static final String FIELD_PUBLISH_ALL_PORTS = "publishAllPorts";
    public static final String FIELD_READ_ONLY = "readOnly";
    public static final String FIELD_REQUESTED_HOST_ID = "requestedHostId";
    public static final String FIELD_REQUESTED_IP_ADDRESS = "requestedIpAddress";
    public static final String FIELD_RESTART_POLICY = "restartPolicy";
    public static final String FIELD_RETAIN_IP = "retainIp";
    public static final String FIELD_REVISION_CONFIG = "config";
    public static final String FIELD_REVISION_ID = "revisionId";
    public static final String FIELD_SECRETS = "secrets";
    public static final String FIELD_SECURITY_OPT = "securityOpt";
    public static final String FIELD_SERVICE_ID = "serviceId";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_SERVICE_INDEX = "serviceIndex";
    public static final String FIELD_SHM_SIZE = "shmSize";
    public static final String FIELD_SHOULD_RESTART = "shouldRestart";
    public static final String FIELD_SIDEKICK_TO = "sidekickTo";
    public static final String FIELD_STACK_ID = "stackId";
    public static final String FIELD_START_RETRY_COUNT = "startRetryCount";
    public static final String FIELD_STDIN_OPEN = "stdinOpen";
    public static final String FIELD_STOPPED = "stopped";
    public static final String FIELD_STOP_SIGNAL = "stopSignal";
    public static final String FIELD_STOP_SOURCE = "stopSource";
    public static final String FIELD_STORAGE_OPT = "storageOpt";
    public static final String FIELD_SYSCTLS = "sysctls";
    public static final String FIELD_SYSTEM_CONTAINER = "systemContainer";
    public static final String FIELD_TMPFS = "tmpfs";
    public static final String FIELD_TTY = "tty";
    public static final String FIELD_ULIMITS = "ulimits";
    public static final String FIELD_USER = "user";
    public static final String FIELD_UTS = "uts";
    public static final String FIELD_VCPU = "vcpu";
    public static final String FIELD_VOLUME_DRIVER = "volumeDriver";
    public static final String FIELD_VOLUMES_FROM = "dataVolumesFrom";
    public static final String FIELD_WORKING_DIR = "workingDir";

    public static final String DOCKER_ATTACH_STDIN = "AttachStdin";
    public static final String DOCKER_ATTACH_STDOUT = "AttachStdout";
    public static final String DOCKER_TTY = "Tty";
    public static final String DOCKER_CMD = "Cmd";
    public static final String DOCKER_CONTAINER = "Container";


    public static final String PULL_NONE = "none";
    public static final String PULL_ALL = "all";
    public static final String PULL_EXISTING = "existing";

    public static final String PROCESS_DATA_NO_OP = "containerNoOpEvent";

    public static final String PROCESS_DATA_ERROR = "errorState";

    public static final String REMOVE_OPTION = "remove";

    public static final String PROCESS_ALLOCATE = "instance.allocate";
    public static final String PROCESS_DEALLOCATE = "instance.deallocate";
    public static final String PROCESS_START = "instance.start";
    public static final String PROCESS_STOP = "instance.stop";

    public static final String KIND_CONTAINER = "container";
    public static final String KIND_VIRTUAL_MACHINE = "virtualMachine";

    public static final String STATE_RUNNING = "running";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_STOPPING = "stopping";
    public static final String STATE_STARTING = "starting";
    public static final String STATE_RESTARTING = "restarting";

    public static final String ON_STOP_REMOVE = "remove";

    public static final String EVENT_INSTANCE_FORCE_STOP = "compute.instance.force.stop";

    public static final String VOLUME_CLEANUP_STRATEGY_NONE = "none";
    public static final String VOLUME_CLEANUP_STRATEGY_UNNAMED = "unnamed";
    public static final String VOLUME_CLEANUP_STRATEGY_ALL = "all";
    public static final Set<String> VOLUME_REMOVE_STRATEGIES = CollectionUtils.set(
            VOLUME_CLEANUP_STRATEGY_NONE,
            VOLUME_CLEANUP_STRATEGY_UNNAMED,
            VOLUME_CLEANUP_STRATEGY_ALL);

    public static boolean isRancherAgent(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        return ("rancher-agent".equals(labels.get("io.rancher.container.system")) &&
                "rancher-agent".equals(instance.getName()));

    }

    public static List<PortSpec> getPortSpecs(Instance instance) {
        List<PortSpec> result = new ArrayList<>();
        for (String portSpec : DataAccessor.fieldStringList(instance, FIELD_PORTS)) {
            try {
                result.add(new PortSpec(portSpec));
            } catch (ClientVisibleException e) {
            }
        }
        return result;
    }

}
