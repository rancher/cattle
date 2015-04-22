package io.cattle.platform.servicediscovery.resource;

import io.cattle.platform.core.constants.InstanceConstants;

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
    public static final ServiceDiscoveryConfigItem VOLUMESFROMSERVICE = new ServiceDiscoveryConfigItem(
            "dataVolumesFromService",
            "volumes_from");
    public static final ServiceDiscoveryConfigItem ENVIRONMENT = new ServiceDiscoveryConfigItem("environment", "environment");
    public static final ServiceDiscoveryConfigItem DNS = new ServiceDiscoveryConfigItem("dns", "dns");
    public static final ServiceDiscoveryConfigItem CAPADD = new ServiceDiscoveryConfigItem("capAdd", "cap_add");
    public static final ServiceDiscoveryConfigItem CAPDROP = new ServiceDiscoveryConfigItem("capDrop", "cap_drop");
    public static final ServiceDiscoveryConfigItem DNSSEARCH = new ServiceDiscoveryConfigItem("dnsSearch", "dns_search");
    public static final ServiceDiscoveryConfigItem WORKINGDIR = new ServiceDiscoveryConfigItem("directory", "working_dir");
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
    public static final ServiceDiscoveryConfigItem EXPOSE = new ServiceDiscoveryConfigItem("expose", "expose");
    public static final ServiceDiscoveryConfigItem EXTERNALLINKS = new ServiceDiscoveryConfigItem("instanceLinks",
            "external_links");
    public static final ServiceDiscoveryConfigItem LINKS = new ServiceDiscoveryConfigItem(null,
            "links", false, true);

    // RANCHER PARAMETERS
    public static final ServiceDiscoveryConfigItem REGISTRYCREDENTIALID = new ServiceDiscoveryConfigItem("registryCredentialId",
            "registryCredentialId", true, false);
    public static final ServiceDiscoveryConfigItem SCALE = new ServiceDiscoveryConfigItem("scale", "scale",
            false, false);
    public static final ServiceDiscoveryConfigItem CPU_SET = new ServiceDiscoveryConfigItem("cpuSet",
            "cpuSet", true, false);
    public static final ServiceDiscoveryConfigItem REQUESTED_HOST_ID = new ServiceDiscoveryConfigItem(
            InstanceConstants.FIELD_REQUESTED_HOST_ID, InstanceConstants.FIELD_REQUESTED_HOST_ID,
            true, false);

    /**
     * Name as it appears in docker-compose file
     */
    private String composeName;

    /**
     * Name as it appears in rancher (can be diff from what defined in a config)
     */
    private String rancherName;

    /**
     * Defines whether the property is the launch config property
     */
    private boolean isLaunchConfigItem;

    private boolean isDockerComposeProperty;

    public ServiceDiscoveryConfigItem(String rancherName, String composeName, boolean isLaunchConfigItem,
            boolean isDockerComposeProperty) {
        super();
        this.rancherName = rancherName;
        this.composeName = composeName;
        this.isLaunchConfigItem = isLaunchConfigItem;
        this.isDockerComposeProperty = isDockerComposeProperty;
        supportedServiceConfigItems.add(this);
    }

    public ServiceDiscoveryConfigItem() {
    }

    public ServiceDiscoveryConfigItem(String rancherName, String composeName) {
        this(rancherName, composeName, true, true);
    }

    public static List<ServiceDiscoveryConfigItem> getSupportedLaunchConfigItems() {
        return supportedServiceConfigItems;
    }

    public String getComposeName() {
        return composeName;
    }

    public boolean isLaunchConfigItem() {
        return isLaunchConfigItem;
    }

    public String getRancherName() {
        return rancherName;
    }

    public boolean isDockerComposeProperty() {
        return isDockerComposeProperty;
    }

    public static ServiceDiscoveryConfigItem getServiceConfigItemByComoposeName(String externalName) {
        for (ServiceDiscoveryConfigItem serviceItem : supportedServiceConfigItems) {
            if (serviceItem.getComposeName().equalsIgnoreCase(externalName)) {
                return serviceItem;
            }
        }
        return null;
    }

    public static ServiceDiscoveryConfigItem getServiceConfigItemByRancherName(String internalName) {
        for (ServiceDiscoveryConfigItem serviceItem : supportedServiceConfigItems) {
            if (serviceItem.getRancherName() != null && serviceItem.getRancherName().equalsIgnoreCase(internalName)) {
                return serviceItem;
            }
        }
        return null;
    }

}
