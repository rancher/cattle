package io.cattle.platform.servicediscovery.api.resource;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceDiscoveryConfigItem {
    /**
     * 
     * Unsupported docker compose variables
     * - build
     * - env_file
     * - net - might add support for that before the feature goes out
     * - environment - don't support the case when only key is specified, but no value - can't extract local machine
     * info here
     *
     */
    // COMPOSE PARAMETERS
    private static List<ServiceDiscoveryConfigItem> supportedServiceConfigItems = new ArrayList<>();
    public static final ServiceDiscoveryConfigItem IMAGE = new ServiceDiscoveryConfigItem(InstanceConstants.FIELD_IMAGE_UUID, "image");
    public static final ServiceDiscoveryConfigItem COMMAND = new ServiceDiscoveryConfigItem("command", "command");
    public static final ServiceDiscoveryConfigItem PORTS = new ServiceDiscoveryConfigItem("ports", "ports");
    public static final ServiceDiscoveryConfigItem VOLUMES = new ServiceDiscoveryConfigItem("dataVolumes", "volumes");
    public static final ServiceDiscoveryConfigItem VOLUMESFROM = new ServiceDiscoveryConfigItem("dataVolumesFrom",
            "volumes_from");
    public static final ServiceDiscoveryConfigItem ENVIRONMENT = new ServiceDiscoveryConfigItem("environment", "environment");
    public static final ServiceDiscoveryConfigItem DNS = new ServiceDiscoveryConfigItem("dns", "dns");
    public static final ServiceDiscoveryConfigItem CAPADD = new ServiceDiscoveryConfigItem("capAdd", "cap_add");
    public static final ServiceDiscoveryConfigItem CAPDROP = new ServiceDiscoveryConfigItem("capDrop", "cap_drop");
    public static final ServiceDiscoveryConfigItem DNSSEARCH = new ServiceDiscoveryConfigItem("dnsSearch", "dns_search");
    public static final ServiceDiscoveryConfigItem WORKINGDIR = new ServiceDiscoveryConfigItem("workingDir", "working_dir");
    public static final ServiceDiscoveryConfigItem ENTRYPOINT = new ServiceDiscoveryConfigItem("entryPoint", "entrypoint");
    public static final ServiceDiscoveryConfigItem USER = new ServiceDiscoveryConfigItem("user", "user");
    public static final ServiceDiscoveryConfigItem HOSTNAME = new ServiceDiscoveryConfigItem("hostname", "hostname");
    public static final ServiceDiscoveryConfigItem DOMAINNAME = new ServiceDiscoveryConfigItem("domainName", "domainname");
    public static final ServiceDiscoveryConfigItem MEMLIMIT = new ServiceDiscoveryConfigItem("memory", "mem_limit");
    public static final ServiceDiscoveryConfigItem PRIVILEGED = new ServiceDiscoveryConfigItem("privileged", "privileged");
    public static final ServiceDiscoveryConfigItem RESTART = new ServiceDiscoveryConfigItem("restartPolicy", "restart");
    public static final ServiceDiscoveryConfigItem STDINOPEN = new ServiceDiscoveryConfigItem("stdinOpen", "stdin_open");
    public static final ServiceDiscoveryConfigItem TTY = new ServiceDiscoveryConfigItem("tty", "tty");
    public static final ServiceDiscoveryConfigItem CPUSHARES = new ServiceDiscoveryConfigItem("cpuShares", "cpu_shares");
    public static final ServiceDiscoveryConfigItem VOLUME_DRIVER = new ServiceDiscoveryConfigItem("volumeDriver", "volume_driver");
    public static final ServiceDiscoveryConfigItem EXPOSE = new ServiceDiscoveryConfigItem("expose", "expose");
    public static final ServiceDiscoveryConfigItem EXTERNALLINKS = new ServiceDiscoveryConfigItem("instanceLinks",
            "external_links");
    public static final ServiceDiscoveryConfigItem LINKS = new ServiceDiscoveryConfigItem(null,
            "links", false, true);
    public static final ServiceDiscoveryConfigItem NETWORKMODE = new ServiceDiscoveryConfigItem(
            DockerInstanceConstants.FIELD_NETWORK_MODE,
            "net");
    public static final ServiceDiscoveryConfigItem CPUSET = new ServiceDiscoveryConfigItem("cpuSet", "cpuset");

    public static final ServiceDiscoveryConfigItem LABELS = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_LABELS, InstanceConstants.FIELD_LABELS);

    // CATTLE PARAMETERS
    public static final ServiceDiscoveryConfigItem SCALE = new ServiceDiscoveryConfigItem("scale", "scale",
            false, false);
    public static final ServiceDiscoveryConfigItem HEALTHCHECK = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_HEALTH_CHECK,
            NamedUtils.toUnderscoreSeparated(InstanceConstants.FIELD_HEALTH_CHECK),
            true, false);

    public static final ServiceDiscoveryConfigItem LB_CONGFIG = new ServiceDiscoveryConfigItem(
            ServiceDiscoveryConstants.FIELD_LOAD_BALANCER_CONFIG,
            NamedUtils.toUnderscoreSeparated(ServiceDiscoveryConstants.FIELD_LOAD_BALANCER_CONFIG),
            false, false);

    public static final ServiceDiscoveryConfigItem EXTERNAL_IPS = new ServiceDiscoveryConfigItem(
            ServiceDiscoveryConstants.FIELD_EXTERNALIPS,
            "external_ips", false, false);

    public static final ServiceDiscoveryConfigItem DEFAULT_CERTIFICATE = new ServiceDiscoveryConfigItem(
            LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID,
            "default_cert", false, false);

    public static final ServiceDiscoveryConfigItem CERTIFICATES = new ServiceDiscoveryConfigItem(
            LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS,
            "certs", false, false);
    public static final ServiceDiscoveryConfigItem METADATA = new ServiceDiscoveryConfigItem(
            ServiceDiscoveryConstants.FIELD_METADATA,
            ServiceDiscoveryConstants.FIELD_METADATA, false, false);

    public static final ServiceDiscoveryConfigItem HAPROXYCONFIG = new ServiceDiscoveryConfigItem(
            LoadBalancerConstants.FIELD_LB_HAPROXY_CONFIG,
            "haproxy_config", false, false);

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

    public ServiceDiscoveryConfigItem(String cattleName, String dockerName, boolean isLaunchConfigItem,
            boolean isDockerComposeProperty) {
        super();
        this.cattleName = cattleName;
        this.dockerName = dockerName;
        this.isLaunchConfigItem = isLaunchConfigItem;
        this.isDockerComposeProperty = isDockerComposeProperty;
        supportedServiceConfigItems.add(this);
    }

    public ServiceDiscoveryConfigItem() {
    }

    public ServiceDiscoveryConfigItem(String cattleName, String dockerName) {
        this(cattleName, dockerName, true, true);
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

    public static ServiceDiscoveryConfigItem getServiceConfigItemByDockerName(String externalName) {
        for (ServiceDiscoveryConfigItem serviceItem : supportedServiceConfigItems) {
            if (serviceItem.getDockerName().equalsIgnoreCase(externalName)) {
                return serviceItem;
            }
        }
        return null;
    }

    public static ServiceDiscoveryConfigItem getServiceConfigItemByCattleName(String internalName, Service service) {
        for (ServiceDiscoveryConfigItem serviceItem : supportedServiceConfigItems) {
            if (serviceItem.getCattleName() != null && serviceItem.getCattleName().equalsIgnoreCase(internalName)) {
                // special handling for external service hostname
                if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.EXTERNALSERVICE.name())
                        && serviceItem.getCattleName().equalsIgnoreCase(HOSTNAME.cattleName)) {
                    return new ServiceDiscoveryConfigItem(serviceItem.getCattleName(), serviceItem.getDockerName(),
                            false, false);
                }
                return serviceItem;
            }
        }
        return null;
    }

}
