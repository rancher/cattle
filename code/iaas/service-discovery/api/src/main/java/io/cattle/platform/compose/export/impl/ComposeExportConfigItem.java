package io.cattle.platform.compose.export.impl;

import io.cattle.platform.core.constants.DockerInstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.List;

public class ComposeExportConfigItem {
    /**
     *
     * Unsupported docker compose variables - build - env_file - net - might add
     * support for that before the feature goes out - environment - don't
     * support the case when only key is specified, but no value - can't extract
     * local machine info here
     *
     */
    // COMPOSE PARAMETERS
    private static List<ComposeExportConfigItem> supportedServiceConfigItems = new ArrayList<>();
    public static final ComposeExportConfigItem IMAGE = new ComposeExportConfigItem(
            InstanceConstants.FIELD_IMAGE_UUID, "image", false);
    public static final ComposeExportConfigItem COMMAND = new ComposeExportConfigItem("command", "command",
            false);
    public static final ComposeExportConfigItem PORTS = new ComposeExportConfigItem("ports", "ports", false);
    public static final ComposeExportConfigItem VOLUMES = new ComposeExportConfigItem("dataVolumes", "volumes",
            false);
    public static final ComposeExportConfigItem VOLUMESFROM = new ComposeExportConfigItem("dataVolumesFrom",
            "volumes_from", false);
    public static final ComposeExportConfigItem ENVIRONMENT = new ComposeExportConfigItem("environment",
            "environment", false);
    public static final ComposeExportConfigItem DNS = new ComposeExportConfigItem("dns", "dns", false);
    public static final ComposeExportConfigItem CAPADD = new ComposeExportConfigItem("capAdd", "cap_add", false);
    public static final ComposeExportConfigItem CAPDROP = new ComposeExportConfigItem("capDrop", "cap_drop",
            false);
    public static final ComposeExportConfigItem DNSSEARCH = new ComposeExportConfigItem("dnsSearch",
            "dns_search",
            false);
    public static final ComposeExportConfigItem WORKINGDIR = new ComposeExportConfigItem("workingDir",
            "working_dir", false);
    public static final ComposeExportConfigItem ENTRYPOINT = new ComposeExportConfigItem("entryPoint",
            "entrypoint", false);
    public static final ComposeExportConfigItem USER = new ComposeExportConfigItem("user", "user", false);
    public static final ComposeExportConfigItem HOSTNAME = new ComposeExportConfigItem("hostname", "hostname",
            false);
    public static final ComposeExportConfigItem DOMAINNAME = new ComposeExportConfigItem("domainName",
            "domainname", false);
    public static final ComposeExportConfigItem MEMLIMIT = new ComposeExportConfigItem("memory", "mem_limit",
            false);
    public static final ComposeExportConfigItem MEMRESERVATION = new ComposeExportConfigItem("memoryReservation",
            "mem_reservation", false);
    public static final ComposeExportConfigItem PRIVILEGED = new ComposeExportConfigItem("privileged",
            "privileged", false);
    public static final ComposeExportConfigItem RESTART = new ComposeExportConfigItem("restartPolicy", "restart",
            false);
    public static final ComposeExportConfigItem STDINOPEN = new ComposeExportConfigItem("stdinOpen",
            "stdin_open",
            false);
    public static final ComposeExportConfigItem SYSCTLS = new ComposeExportConfigItem("sysctls", "sysctls", false);
    public static final ComposeExportConfigItem TTY = new ComposeExportConfigItem("tty", "tty", false);
    public static final ComposeExportConfigItem CPUSHARES = new ComposeExportConfigItem("cpuShares",
            "cpu_shares",
            false);
    public static final ComposeExportConfigItem BLKIOWEIGHT = new ComposeExportConfigItem("blkioWeight",
            "blkio_weight", false);
    public static final ComposeExportConfigItem CGROUPPARENT = new ComposeExportConfigItem("cgroupParent",
            "cgroup_parent", false);
    public static final ComposeExportConfigItem CPUPERIOD = new ComposeExportConfigItem("cpuPeriod",
            "cpu_period",
            false);
    public static final ComposeExportConfigItem CPUQUOTA = new ComposeExportConfigItem("cpuQuota", "cpu_quota",
            false);
    public static final ComposeExportConfigItem DNSOPT = new ComposeExportConfigItem("dnsOpt", "dns_opt", false);
    public static final ComposeExportConfigItem GROUPADD = new ComposeExportConfigItem("groupAdd", "group_add",
            false);
    public static final ComposeExportConfigItem EXTRAHOSTS = new ComposeExportConfigItem("extraHosts",
            "extra_hosts", false);
    public static final ComposeExportConfigItem SECURITYOPT = new ComposeExportConfigItem("securityOpt",
            "security_opt", false);
    public static final ComposeExportConfigItem READONLY = new ComposeExportConfigItem("readOnly", "read_only",
            false);
    public static final ComposeExportConfigItem MEMORYSWAPPINESS = new ComposeExportConfigItem(
            "memorySwappiness",
            "mem_swappiness", false);
    public static final ComposeExportConfigItem OOMKILLDISABLE = new ComposeExportConfigItem("oomKillDisable",
            "oom_kill_disable", false);
    public static final ComposeExportConfigItem SHMSIZE = new ComposeExportConfigItem("shmSize", "shm_size",
            false);
    public static final ComposeExportConfigItem UTS = new ComposeExportConfigItem("uts", "uts", false);
    public static final ComposeExportConfigItem STOPSIGNAL = new ComposeExportConfigItem("stopSignal",
            "stop_signal", false);
    public static final ComposeExportConfigItem OOMSCOREADJ = new ComposeExportConfigItem("oomScoreAdj",
            "oom_score_adj", false);
    public static final ComposeExportConfigItem IPCMODE = new ComposeExportConfigItem("ipcMode", "ipc",
            false);
    public static final ComposeExportConfigItem ISOLATION = new ComposeExportConfigItem("isolation", "isolation",
            false);
    public static final ComposeExportConfigItem MILLICPURESERVATION = new ComposeExportConfigItem(
            "milliCpuReservation", "milli_cpu_reservation", true, false, false);
    public static final ComposeExportConfigItem VOLUME_DRIVER = new ComposeExportConfigItem("volumeDriver",
            "volume_driver", false);
    public static final ComposeExportConfigItem EXPOSE = new ComposeExportConfigItem("expose", "expose", false);
    public static final ComposeExportConfigItem EXTERNALLINKS = new ComposeExportConfigItem("instanceLinks",
            "external_links", false);
    public static final ComposeExportConfigItem LINKS = new ComposeExportConfigItem(null, "links", false, true,
            false);
    public static final ComposeExportConfigItem NETWORKMODE = new ComposeExportConfigItem(
            DockerInstanceConstants.FIELD_NETWORK_MODE,
            NamedUtils.toUnderscoreSeparated(DockerInstanceConstants.FIELD_NETWORK_MODE), false);
    public static final ComposeExportConfigItem CPUSET = new ComposeExportConfigItem("cpuSet", "cpuset", false);

    public static final ComposeExportConfigItem LABELS = new ComposeExportConfigItem(
            InstanceConstants.FIELD_LABELS, InstanceConstants.FIELD_LABELS, false);
    public static final ComposeExportConfigItem MEMSWAPLIMIT = new ComposeExportConfigItem(
            DockerInstanceConstants.FIELD_MEMORY_SWAP, "memswap_limit", false);
    public static final ComposeExportConfigItem PIDMODE = new ComposeExportConfigItem(
            DockerInstanceConstants.FIELD_PID_MODE, "pid", false);
    public static final ComposeExportConfigItem DEVICES = new ComposeExportConfigItem(
            DockerInstanceConstants.FIELD_DEVICES, DockerInstanceConstants.FIELD_DEVICES, false);

    // CATTLE PARAMETERS
    public static final ComposeExportConfigItem SCALE = new ComposeExportConfigItem(ServiceConstants.FIELD_SCALE,
            ServiceConstants.FIELD_SCALE, false,
            false, false);
    public static final ComposeExportConfigItem SCALE_MIN = new ComposeExportConfigItem(
            ServiceConstants.FIELD_SCALE_MIN, NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_SCALE_MIN),
            false,
            false, false);
    public static final ComposeExportConfigItem SCALE_MAX = new ComposeExportConfigItem(
            ServiceConstants.FIELD_SCALE_MAX, NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_SCALE_MAX),
            false,
            false, false);
    public static final ComposeExportConfigItem SCALE_INCREMENT = new ComposeExportConfigItem(
            ServiceConstants.FIELD_SCALE_INCREMENT,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_SCALE_INCREMENT), false,
            false, false);
    public static final ComposeExportConfigItem STARTONCREATE = new ComposeExportConfigItem(
            ServiceConstants.FIELD_START_ON_CREATE,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_START_ON_CREATE), true, false, false);
    public static final ComposeExportConfigItem HEALTHCHECK = new ComposeExportConfigItem(
            InstanceConstants.FIELD_HEALTH_CHECK,
            NamedUtils.toUnderscoreSeparated(InstanceConstants.FIELD_HEALTH_CHECK), true, false, false);

    public static final ComposeExportConfigItem LB_CONGFIG = new ComposeExportConfigItem(
            ServiceConstants.FIELD_LOAD_BALANCER_CONFIG,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_LOAD_BALANCER_CONFIG), false, false, false);

    public static final ComposeExportConfigItem EXTERNAL_IPS = new ComposeExportConfigItem(
            ServiceConstants.FIELD_EXTERNALIPS, "external_ips", false, false, false);

    public static final ComposeExportConfigItem DEFAULT_CERTIFICATE = new ComposeExportConfigItem(
            LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID, "default_cert", false, false, false);

    public static final ComposeExportConfigItem SERVICE_TYPE = new ComposeExportConfigItem("kind", "type", false,
            false, false);

    public static final ComposeExportConfigItem CERTIFICATES = new ComposeExportConfigItem(
            LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS, "certs", false, false, false);
    public static final ComposeExportConfigItem METADATA = new ComposeExportConfigItem(
            ServiceConstants.FIELD_METADATA, ServiceConstants.FIELD_METADATA, false, false, false);
    public static final ComposeExportConfigItem RETAIN_IP = new ComposeExportConfigItem(
            ServiceConstants.FIELD_RETAIN_IP, "retain_ip", false, false, false);

    public static final ComposeExportConfigItem LB_CONFIG = new ComposeExportConfigItem(
            ServiceConstants.FIELD_LB_CONFIG,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_LB_CONFIG), false, false, false);
    public static final ComposeExportConfigItem NETWORK_DRIVER = new ComposeExportConfigItem(
            ServiceConstants.FIELD_NETWORK_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_NETWORK_DRIVER), false, false, false);
    public static final ComposeExportConfigItem STORAGE_DRIVER = new ComposeExportConfigItem(
            ServiceConstants.FIELD_STORAGE_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_STORAGE_DRIVER), false, false, false);

    // VOLUME parameter
    private static List<ComposeExportConfigItem> supportedVolumeConfigItems = new ArrayList<>();

    public static final ComposeExportConfigItem DRIVER = new ComposeExportConfigItem(
            ServiceConstants.FIELD_VOLUME_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_DRIVER), true);
    public static final ComposeExportConfigItem DRIVER_OPTS = new ComposeExportConfigItem(
            ServiceConstants.FIELD_VOLUME_DRIVER_OPTS,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_DRIVER_OPTS), true);
    public static final ComposeExportConfigItem EXTERNAL = new ComposeExportConfigItem(
            ServiceConstants.FIELD_VOLUME_EXTERNAL,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_EXTERNAL), true);
    public static final ComposeExportConfigItem PER_CONTAINER = new ComposeExportConfigItem(
            ServiceConstants.FIELD_VOLUME_PER_CONTAINER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_PER_CONTAINER), true);

    /**
     * Name as it appears in docker-compose file
     */
    private String dockerName;

    /**
     * Name as it appears in cattle (can be diff from what defined in a config)
     */
    private String cattleName;

    /**
     * Defines whether the property is the launch config property
     */
    private boolean isLaunchConfigItem;

    private boolean isDockerComposeProperty;

    private boolean isVolume;

    public ComposeExportConfigItem(String cattleName, String dockerName, boolean isLaunchConfigItem,
            boolean isDockerComposeProperty, boolean isVolume) {
        super();
        this.cattleName = cattleName;
        this.dockerName = dockerName;
        this.isLaunchConfigItem = isLaunchConfigItem;
        this.isDockerComposeProperty = isDockerComposeProperty;
        if (isVolume) {
            supportedVolumeConfigItems.add(this);
        } else {
            supportedServiceConfigItems.add(this);
        }
    }

    public ComposeExportConfigItem() {
    }

    public ComposeExportConfigItem(String cattleName, String dockerName, boolean isVolume) {
        this(cattleName, dockerName, true, true, isVolume);
    }

    public static List<ComposeExportConfigItem> getSupportedLaunchConfigItems() {
        return supportedServiceConfigItems;
    }

    public String getDockerName() {
        return dockerName;
    }

    public boolean isLaunchConfigItem() {
        return isLaunchConfigItem;
    }

    public String getCattleName() {
        return cattleName;
    }

    public boolean isDockerComposeProperty() {
        return isDockerComposeProperty;
    }

    public boolean isVolume() {
        return isVolume;
    }

    public static ComposeExportConfigItem getServiceConfigItemByCattleName(String internalName, Service service,
            boolean isVolume) {
        List<ComposeExportConfigItem> items = new ArrayList<>();
        if (isVolume) {
            items = supportedVolumeConfigItems;
        } else {
            items = supportedServiceConfigItems;
        }
        for (ComposeExportConfigItem serviceItem : items) {
            if (serviceItem.getCattleName() != null && serviceItem.getCattleName().equalsIgnoreCase(internalName)) {
                // special handling for external service hostname
                if (!isVolume && service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                        && serviceItem.getCattleName().equalsIgnoreCase(HOSTNAME.cattleName)) {
                    return new ComposeExportConfigItem(serviceItem.getCattleName(), serviceItem.getDockerName(),
                            false, false, false);
                }
                return serviceItem;
            }
        }
        return null;
    }

}
