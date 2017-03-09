package io.cattle.platform.servicediscovery.api.export.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceDiscoveryConfigItem {
    /**
     *
     * Unsupported docker compose variables - build - env_file - net - might add
     * support for that before the feature goes out - environment - don't
     * support the case when only key is specified, but no value - can't extract
     * local machine info here
     *
     */
    // COMPOSE PARAMETERS
    private static List<ServiceDiscoveryConfigItem> supportedServiceConfigItems = new ArrayList<>();
    public static final ServiceDiscoveryConfigItem IMAGE = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_IMAGE_UUID, "image", false);
    public static final ServiceDiscoveryConfigItem COMMAND = new ServiceDiscoveryConfigItem("command", "command",
            false);
    public static final ServiceDiscoveryConfigItem PORTS = new ServiceDiscoveryConfigItem("ports", "ports", false);
    public static final ServiceDiscoveryConfigItem VOLUMES = new ServiceDiscoveryConfigItem("dataVolumes", "volumes",
            false);
    public static final ServiceDiscoveryConfigItem VOLUMESFROM = new ServiceDiscoveryConfigItem("dataVolumesFrom",
            "volumes_from", false);
    public static final ServiceDiscoveryConfigItem ENVIRONMENT = new ServiceDiscoveryConfigItem("environment",
            "environment", false);
    public static final ServiceDiscoveryConfigItem DNS = new ServiceDiscoveryConfigItem("dns", "dns", false);
    public static final ServiceDiscoveryConfigItem CAPADD = new ServiceDiscoveryConfigItem("capAdd", "cap_add", false);
    public static final ServiceDiscoveryConfigItem CAPDROP = new ServiceDiscoveryConfigItem("capDrop", "cap_drop",
            false);
    public static final ServiceDiscoveryConfigItem DNSSEARCH = new ServiceDiscoveryConfigItem("dnsSearch", "dns_search",
            false);
    public static final ServiceDiscoveryConfigItem WORKINGDIR = new ServiceDiscoveryConfigItem("workingDir",
            "working_dir", false);
    public static final ServiceDiscoveryConfigItem ENTRYPOINT = new ServiceDiscoveryConfigItem("entryPoint",
            "entrypoint", false);
    public static final ServiceDiscoveryConfigItem USER = new ServiceDiscoveryConfigItem("user", "user", false);
    public static final ServiceDiscoveryConfigItem HOSTNAME = new ServiceDiscoveryConfigItem("hostname", "hostname",
            false);
    public static final ServiceDiscoveryConfigItem DOMAINNAME = new ServiceDiscoveryConfigItem("domainName",
            "domainname", false);
    public static final ServiceDiscoveryConfigItem MEMLIMIT = new ServiceDiscoveryConfigItem("memory", "mem_limit",
            false);
    public static final ServiceDiscoveryConfigItem MEMRESERVATION = new ServiceDiscoveryConfigItem("memoryReservation",
            "mem_reservation", false);
    public static final ServiceDiscoveryConfigItem PRIVILEGED = new ServiceDiscoveryConfigItem("privileged",
            "privileged", false);
    public static final ServiceDiscoveryConfigItem RESTART = new ServiceDiscoveryConfigItem("restartPolicy", "restart",
            false);
    public static final ServiceDiscoveryConfigItem STDINOPEN = new ServiceDiscoveryConfigItem("stdinOpen", "stdin_open",
            false);
    public static final ServiceDiscoveryConfigItem SECRETS = new ServiceDiscoveryConfigItem("secrets", "secrets",
            false);
    public static final ServiceDiscoveryConfigItem TTY = new ServiceDiscoveryConfigItem("tty", "tty", false);
    public static final ServiceDiscoveryConfigItem CPUSHARES = new ServiceDiscoveryConfigItem("cpuShares", "cpu_shares",
            false);
    public static final ServiceDiscoveryConfigItem BLKIOWEIGHT = new ServiceDiscoveryConfigItem("blkioWeight",
            "blkio_weight", false);
    public static final ServiceDiscoveryConfigItem CGROUPPARENT = new ServiceDiscoveryConfigItem("cgroupParent",
            "cgroup_parent", false);
    public static final ServiceDiscoveryConfigItem CPUPERIOD = new ServiceDiscoveryConfigItem("cpuPeriod", "cpu_period",
            false);
    public static final ServiceDiscoveryConfigItem CPUQUOTA = new ServiceDiscoveryConfigItem("cpuQuota", "cpu_quota",
            false);
    public static final ServiceDiscoveryConfigItem DNSOPT = new ServiceDiscoveryConfigItem("dnsOpt", "dns_opt", false);
    public static final ServiceDiscoveryConfigItem GROUPADD = new ServiceDiscoveryConfigItem("groupAdd", "group_add",
            false);
    public static final ServiceDiscoveryConfigItem EXTRAHOSTS = new ServiceDiscoveryConfigItem("extraHosts", "extra_hosts", false);
    public static final ServiceDiscoveryConfigItem SECURITYOPT = new ServiceDiscoveryConfigItem("securityOpt", "security_opt", false);
    public static final ServiceDiscoveryConfigItem READONLY = new ServiceDiscoveryConfigItem("readOnly", "read_only", false);
    public static final ServiceDiscoveryConfigItem MEMORYSWAPPINESS = new ServiceDiscoveryConfigItem("memorySwappiness",
            "mem_swappiness", false);
    public static final ServiceDiscoveryConfigItem OOMKILLDISABLE = new ServiceDiscoveryConfigItem("oomKillDisable",
            "oom_kill_disable", false);
    public static final ServiceDiscoveryConfigItem SHMSIZE = new ServiceDiscoveryConfigItem("shmSize", "shm_size",
            false);
    public static final ServiceDiscoveryConfigItem UTS = new ServiceDiscoveryConfigItem("uts", "uts", false);
    public static final ServiceDiscoveryConfigItem STOPSIGNAL = new ServiceDiscoveryConfigItem("stopSignal",
            "stop_signal", false);
    public static final ServiceDiscoveryConfigItem OOMSCOREADJ = new ServiceDiscoveryConfigItem("oomScoreAdj",
            "oom_score_adj", false);
    public static final ServiceDiscoveryConfigItem IPCMODE = new ServiceDiscoveryConfigItem("ipcMode", "ipc",
            false);
    public static final ServiceDiscoveryConfigItem ISOLATION = new ServiceDiscoveryConfigItem("isolation", "isolation",
            false);
    public static final ServiceDiscoveryConfigItem MILLICPURESERVATION = new ServiceDiscoveryConfigItem(
            "milliCpuReservation", "milli_cpu_reservation", true, false, false);
    public static final ServiceDiscoveryConfigItem VOLUME_DRIVER = new ServiceDiscoveryConfigItem("volumeDriver",
            "volume_driver", false);
    public static final ServiceDiscoveryConfigItem EXPOSE = new ServiceDiscoveryConfigItem("expose", "expose", false);
    public static final ServiceDiscoveryConfigItem EXTERNALLINKS = new ServiceDiscoveryConfigItem("instanceLinks",
            "external_links", false);
    public static final ServiceDiscoveryConfigItem LINKS = new ServiceDiscoveryConfigItem(null, "links", false, true,
            false);
    public static final ServiceDiscoveryConfigItem NETWORKMODE = new ServiceDiscoveryConfigItem(
            DockerInstanceConstants.FIELD_NETWORK_MODE,
            NamedUtils.toUnderscoreSeparated(DockerInstanceConstants.FIELD_NETWORK_MODE), false);
    public static final ServiceDiscoveryConfigItem CPUSET = new ServiceDiscoveryConfigItem("cpuSet", "cpuset", false);

    public static final ServiceDiscoveryConfigItem LABELS = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_LABELS, InstanceConstants.FIELD_LABELS, false);
    public static final ServiceDiscoveryConfigItem MEMSWAPLIMIT = new ServiceDiscoveryConfigItem(
            DockerInstanceConstants.FIELD_MEMORY_SWAP, "memswap_limit", false);
    public static final ServiceDiscoveryConfigItem PIDMODE = new ServiceDiscoveryConfigItem(
            DockerInstanceConstants.FIELD_PID_MODE, "pid", false);
    public static final ServiceDiscoveryConfigItem DEVICES = new ServiceDiscoveryConfigItem(
            DockerInstanceConstants.FIELD_DEVICES, DockerInstanceConstants.FIELD_DEVICES, false);

    // CATTLE PARAMETERS
    public static final ServiceDiscoveryConfigItem SCALE = new ServiceDiscoveryConfigItem("scale", "scale", false,
            false, false);
    public static final ServiceDiscoveryConfigItem STARTONCREATE = new ServiceDiscoveryConfigItem(ServiceConstants.FIELD_START_ON_CREATE,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_START_ON_CREATE), true, false, false);
    public static final ServiceDiscoveryConfigItem HEALTHCHECK = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_HEALTH_CHECK,
            NamedUtils.toUnderscoreSeparated(InstanceConstants.FIELD_HEALTH_CHECK), true, false, false);

    public static final ServiceDiscoveryConfigItem LB_CONGFIG = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_LOAD_BALANCER_CONFIG,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_LOAD_BALANCER_CONFIG), false, false, false);

    public static final ServiceDiscoveryConfigItem EXTERNAL_IPS = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_EXTERNALIPS, "external_ips", false, false, false);

    public static final ServiceDiscoveryConfigItem DEFAULT_CERTIFICATE = new ServiceDiscoveryConfigItem(
            LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID, "default_cert", false, false, false);

    public static final ServiceDiscoveryConfigItem SERVICE_TYPE = new ServiceDiscoveryConfigItem("kind", "type", false,
            false, false);

    public static final ServiceDiscoveryConfigItem CERTIFICATES = new ServiceDiscoveryConfigItem(
            LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS, "certs", false, false, false);
    public static final ServiceDiscoveryConfigItem METADATA = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_METADATA, ServiceConstants.FIELD_METADATA, false, false, false);
    public static final ServiceDiscoveryConfigItem RETAIN_IP = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_SERVICE_RETAIN_IP, "retain_ip", false, false, false);

    public static final ServiceDiscoveryConfigItem LB_CONFIG = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_LB_CONFIG,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_LB_CONFIG), false, false, false);
    public static final ServiceDiscoveryConfigItem NETWORK_DRIVER = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_NETWORK_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_NETWORK_DRIVER), false, false, false);
    public static final ServiceDiscoveryConfigItem STORAGE_DRIVER = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_STORAGE_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_STORAGE_DRIVER), false, false, false);

    // VOLUME parameter
    private static List<ServiceDiscoveryConfigItem> supportedVolumeConfigItems = new ArrayList<>();

    public static final ServiceDiscoveryConfigItem DRIVER = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_VOLUME_DRIVER,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_DRIVER), true);
    public static final ServiceDiscoveryConfigItem DRIVER_OPTS = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_VOLUME_DRIVER_OPTS,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_DRIVER_OPTS), true);
    public static final ServiceDiscoveryConfigItem EXTERNAL = new ServiceDiscoveryConfigItem(
            ServiceConstants.FIELD_VOLUME_EXTERNAL,
            NamedUtils.toUnderscoreSeparated(ServiceConstants.FIELD_VOLUME_EXTERNAL), true);
    public static final ServiceDiscoveryConfigItem PER_CONTAINER = new ServiceDiscoveryConfigItem(
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

    public ServiceDiscoveryConfigItem(String cattleName, String dockerName, boolean isLaunchConfigItem,
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

    public ServiceDiscoveryConfigItem() {
    }

    public ServiceDiscoveryConfigItem(String cattleName, String dockerName, boolean isVolume) {
        this(cattleName, dockerName, true, true, isVolume);
    }

    public static List<ServiceDiscoveryConfigItem> getSupportedLaunchConfigItems() {
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

    public static ServiceDiscoveryConfigItem getServiceConfigItemByCattleName(String internalName, Service service,
            boolean isVolume) {
        List<ServiceDiscoveryConfigItem> items = new ArrayList<>();
        if (isVolume) {
            items = supportedVolumeConfigItems;
        } else {
            items = supportedServiceConfigItems;
        }
        for (ServiceDiscoveryConfigItem serviceItem : items) {
            if (serviceItem.getCattleName() != null && serviceItem.getCattleName().equalsIgnoreCase(internalName)) {
                // special handling for external service hostname
                if (!isVolume && service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                        && serviceItem.getCattleName().equalsIgnoreCase(HOSTNAME.cattleName)) {
                    return new ServiceDiscoveryConfigItem(serviceItem.getCattleName(), serviceItem.getDockerName(),
                            false, false, false);
                }
                return serviceItem;
            }
        }
        return null;
    }

}
