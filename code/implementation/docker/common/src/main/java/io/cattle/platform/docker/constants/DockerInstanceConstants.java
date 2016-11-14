package io.cattle.platform.docker.constants;

public class DockerInstanceConstants {

    public static final String FIELD_BUILD = "build";
    public static final String FIELD_DOCKER_PORTS = "dockerPorts";
    public static final String FIELD_DOCKER_HOST_IP = "dockerHostIp";
    public static final String FIELD_DOCKER_IP = "dockerIp";
    public static final String FIELD_DOCKER_INSPECT = "dockerInspect";
    public static final String FIELD_DOCKER_MOUNTS = "dockerMounts";
    public static final String FIELD_VOLUMES_FROM = "dataVolumesFrom";
    public static final String FIELD_DOMAIN_NAME = "domainName";
    public static final String FIELD_USER = "user";
    public static final String FIELD_MEMORY_SWAP = "memorySwap";
    public static final String FIELD_NETWORK_MODE = "networkMode";
    public static final String FIELD_NETWORK_CONTAINER_ID = "networkContainerId";
    public static final String FIELD_CPU_SHARES = "cpuShares";
    public static final String FIELD_CPU_SET = "cpuSet";
    public static final String FIELD_TTY = "tty";
    public static final String FIELD_STDIN_OPEN = "stdinOpen";
    public static final String FIELD_COMMAND = "command";
    public static final String FIELD_ENTRY_POINT = "entryPoint";
    public static final String FIELD_PUBLISH_ALL_PORTS = "publishAllPorts";
    public static final String FIELD_LXC_CONF = "lxcConf";
    public static final String FIELD_DNS = "dns";
    public static final String FIELD_DNS_SEARCH = "dnsSearch";
    public static final String FIELD_CAP_ADD = "capAdd";
    public static final String FIELD_CAP_DROP = "capDrop";
    public static final String FIELD_RESTART_POLICY = "restartPolicy";
    public static final String FIELD_DEVICES = "devices";
    public static final String FIELD_WORKING_DIR = "workingDir";
    public static final String FIELD_SECURITY_OPT = "securityOpt";
    public static final String FIELD_PID_MODE = "pidMode";
    public static final String FIELD_EXTRA_HOSTS = "extraHosts";
    public static final String FIELD_READ_ONLY = "readOnly";
    public static final String FIELD_BLKIO_DEVICE_OPTIONS = "blkioDeviceOptions";
    public static final String FIELD_BLKIO_WEIGHT = "blkioWeight";
    public static final String FIELD_CGROUP_PARENT = "cgroupParent";
    public static final String FIELD_CPU_PERIOD = "cpuPeriod";
    public static final String FIELD_CPU_QUOTA = "cpuQuota";
    public static final String FIELD_CPUSET_MEMS = "cpuSetMems";
    public static final String FIELD_DNS_OPT = "dnsOpt";
    public static final String FIELD_GROUP_ADD = "groupAdd";
    public static final String FIELD_KERNEL_MEMORY = "kernelMemory";
    public static final String FIELD_MEMORY_SWAPPINESS = "memorySwappiness";
    public static final String FIELD_OOMKILL_DISABLE = "oomKillDisable";
    public static final String FIELD_SHM_SIZE = "shmSize";
    public static final String FIELD_TMPFS = "tmpfs";
    public static final String FIELD_UTS = "uts";
    public static final String FIELD_IPC_MODE = "ipcMode";
    public static final String FIELD_STOP_SIGNAL = "stopSignal";
    public static final String FIELD_SYSCTLS = "sysctls";
    public static final String FIELD_STORAGE_OPT = "storageOpt";
    public static final String FIELD_OOM_SCORE_ADJ = "oomScoreAdj";
    public static final String FIELD_ULIMITS = "ulimits";
    public static final String FIELD_ISOLATION = "isolation";

    public static final String EVENT_FIELD_VOLUMES_FROM = "dataVolumesFromContainers";
    public static final String EVENT_FIELD_VOLUMES_FROM_DVM = "volumesFromDataVolumeMounts";

    public static final String DOCKER_ATTACH_STDIN = "AttachStdin";
    public static final String DOCKER_ATTACH_STDOUT = "AttachStdout";
    public static final String DOCKER_TTY = "Tty";
    public static final String DOCKER_CMD = "Cmd";
    public static final String DOCKER_CONTAINER = "Container";

}