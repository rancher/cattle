package io.cattle.platform.servicediscovery.api.constants;


public class ServiceDiscoveryConstants {

    public enum KIND {
        SERVICE,
        LOADBALANCERSERVICE,
        EXTERNALSERVICE,
        DNSSERVICE
    }

    public static final String FIELD_SCALE = "scale";
    public static final String FIELD_NETWORK_ID = "networkId";
    public static final String FIELD_SERVICE_ID = "serviceId";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_LAUNCH_CONFIG = "launchConfig";
    public static final String FIELD_LOAD_BALANCER_CONFIG = "loadBalancerConfig";
    public static final String FIELD_ENVIRIONMENT_ID = "environmentId";
    public static final String FIELD_EXTERNALIPS = "externalIpAddresses";
    public static final String FIELD_SERVICE_LINKS = "serviceLinks";
    public static final String FIELD_SERVICE_LINK_NAME = "name";
    public static final String FIELD_NETWORK_LAUNCH_CONFIG = "networkLaunchConfig";
    public static final String FIELD_SECONDARY_LAUNCH_CONFIGS = "secondaryLaunchConfigs";
    public static final String FIELD_DATA_VOLUMES_LAUNCH_CONFIG = "dataVolumesFromLaunchConfigs";

    public static final String ACTION_SERVICE_ACTIVATE = "activate";
    public static final String ACTION_SERVICE_CREATE = "create";
    public static final String ACTION_SERVICE_ADD_SERVICE_LINK = "addservicelink";
    public static final String ACTION_SERVICE_REMOVE_SERVICE_LINK = "removeservicelink";
    public static final String ACTION_ENV_ACTIVATE_SERVICES = "activateservices";
    public static final String ACTION_SERVICE_SET_SERVICE_LINKS = "setservicelinks";

    public static final String PROCESS_SERVICE_ACTIVATE = "service." + ACTION_SERVICE_ACTIVATE;
    public static final String PROCESS_SERVICE_CREATE = "service." + ACTION_SERVICE_CREATE;
    public static final String PROCESS_ENV_ACTIVATE_SERVICES = "environment." + ACTION_ENV_ACTIVATE_SERVICES;
    public static final String PROCESS_ENV_DEACTIVATE_SERVICES = "environment.deactivateservices";
    public static final String PROCESS_ENV_UPDATE = "environment.update";
    public static final String PROCESS_ENV_REMOVE = "environment.remove";
    public static final String PROCESS_SERVICE_DEACTIVATE = "service.deactivate";
    public static final String PROCESS_SERVICE_REMOVE = "service.remove";
    public static final String PROCESS_ENV_EXPORT_CONFIG = "environment.exportconfig";
    public static final String PROCESS_SERVICE_ADD_SERVICE_LINK = "service." + ACTION_SERVICE_ADD_SERVICE_LINK;
    public static final String PROCESS_SERVICE_REMOVE_SERVICE_LINK = "service." + ACTION_SERVICE_REMOVE_SERVICE_LINK;
    public static final String PROCESS_SERVICE_CONSUME_MAP_CREATE = "serviceconsumemap.create";
    public static final String PROCESS_SERVICE_CONSUME_MAP_REMOVE = "serviceconsumemap.remove";
    public static final String PROCESS_SERVICE_UPDATE = "service.update";
    public static final String PROCESS_SERVICE_SET_SERVICE_LINKS = "service." + ACTION_SERVICE_SET_SERVICE_LINKS;
    public static final String PROCESS_SERVICE_EXPOSE_MAP_CREATE = "serviceexposemap.create";
    public static final String PROCESS_SERVICE_EXPOSE_MAP_REMOVE = "serviceexposemap.remove";

    public static final String LINK_DOCKER_COMPOSE_CONFIG = "dockerComposeConfig";
    public static final String LINK_RANCHER_COMPOSE_CONFIG = "rancherComposeConfig";
    public static final String LINK_COMPOSE_CONFIG = "composeConfig";

    public static final String LABEL_SERVICE_NAME = "io.rancher.service.name";
    public static final String LABEL_SERVICE_DEPLOYMENT_UNIT = "io.rancher.service.deployment.unit";
    public static final String LABEL_ENVIRONMENT_NAME = "io.rancher.environment.name";
    public static final String LABEL_SERVICE_GLOBAL = "io.rancher.scheduler.global";
    public static final String LABEL_SERVICE_REQUESTED_HOST_ID = "io.rancher.service.requested.host.id";
    public static final String LABEL_SERVICE_LAUNCH_CONFIG = "io.rancher.service.launch.config";

    public static final String PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";

}
