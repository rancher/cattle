package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ServiceConstants {

    public static final String KIND_SERVICE = "service";
    public static final String KIND_LOAD_BALANCER_SERVICE = "loadBalancerService";
    public static final String KIND_EXTERNAL_SERVICE = "externalService";
    public static final String KIND_DNS_SERVICE = "dnsService";
    public static final String KIND_STORAGE_DRIVER_SERVICE = "storageDriverService";
    public static final String KIND_NETWORK_DRIVER_SERVICE = "networkDriverService";
    public static final Set<String> SERVICE_LIKE = new HashSet<>(Arrays.asList(
            KIND_SERVICE,
            KIND_LOAD_BALANCER_SERVICE,
            KIND_STORAGE_DRIVER_SERVICE,
            KIND_NETWORK_DRIVER_SERVICE
        ));

    public static final String TYPE_STACK = "stack";
    public static final String FIELD_SCALE = "scale";
    public static final String FIELD_NETWORK_ID = "networkId";
    public static final String FIELD_SERVICE_ID = "serviceId";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_UPGRADE = "upgrade";
    public static final String FIELD_LAUNCH_CONFIG = "launchConfig";
    public static final String FIELD_LOG_CONFIG = "logConfig";
    public static final String FIELD_TMPFS= "tmpfs";
    public static final String FIELD_ULIMITS = "ulimits";
    public static final String FIELD_BLKIOOPTIONS = "blkioDeviceOptions";
    public static final String FIELD_LOAD_BALANCER_CONFIG = "loadBalancerConfig";
    public static final String FIELD_EXTERNALIPS = "externalIpAddresses";
    public static final String FIELD_LINKED_SERVICES = "linkedServices";
    public static final String FIELD_SERVICE_LINKS = "serviceLinks";
    public static final String FIELD_SERVICE_LINK_NAME = "name";
    public static final String FIELD_NETWORK_LAUNCH_CONFIG = "networkLaunchConfig";
    public static final String FIELD_SECONDARY_LAUNCH_CONFIGS = "secondaryLaunchConfigs";
    public static final String FIELD_DATA_VOLUMES_LAUNCH_CONFIG = "dataVolumesFromLaunchConfigs";
    public static final String FIELD_WAIT_FOR_CONSUMED_SERVICES_IDS = "waitForConsumedServicesIds";
    public static final String FIELD_SERVICE_LINK = "serviceLink";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_VIP = "vip";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_SELECTOR_CONTAINER = "selectorContainer";
    public static final String FIELD_SELECTOR_LINK = "selectorLink";
    public static final String FIELD_START_ON_CREATE = "startOnCreate";
    public static final String FIELD_IN_SERVICE_STRATEGY = "inServiceStrategy";
    public static final String FIELD_TO_SERVICE_STRATEGY = "toServiceStrategy";
    public static final String FIELD_FQDN = "fqdn";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_PUBLIC_ENDPOINTS = "publicEndpoints";
    public static final String FIELD_RESTART = "restart";
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_SERVICE_RETAIN_IP = "retainIp";
    public static final String STACK_FIELD_DOCKER_COMPOSE = "dockerCompose";
    public static final String STACK_FIELD_RANCHER_COMPOSE = "rancherCompose";
    public static final String STACK_FIELD_START_ON_CREATE = "startOnCreate";
    public static final String STACK_FIELD_SERVICE_IDS = "serviceIds";
    public static final String STACK_FIELD_ENVIRONMENT = "environment";
    public static final String STACK_FIELD_BINDING = "binding";
    public static final String FIELD_SET_VIP = "assignServiceIpAddress";
    public static final String FIELD_SCALE_POLICY = "scalePolicy";
    public static final String FIELD_DESIRED_SCALE = "desiredScale";
    public static final String FIELD_CURRENT_SCALE = "currentScale";
    public static final String FIELD_HEALTH_STATE = "healthState";
    public static final String FIELD_LOCKED_SCALE = "lockedScale";
    public static final String FIELD_STACK_ID = "stackId";
    public static final String FIELD_SYSTEM = "system";
    public static final String FIELD_VOLUME_TEMPLATES = "volumeTemplates";
    public static final String FIELD_VOLUME_DRIVER = "driver";
    public static final String FIELD_VOLUME_DRIVER_OPTS = "driverOpts";
    public static final String FIELD_VOLUME_EXTERNAL = "external";
    public static final String FIELD_VOLUME_PER_CONTAINER = "perContainer";
    public static final String FIELD_INSTANCE_IDS = "instanceIds";
    public static final String FIELD_LB_CONFIG = "lbConfig";
    public static final String FIELD_PORT_RULES = "portRules";

    public static final String FIELD_INTERNAL_VOLUMES = "internalVolumes";
    public static final String FIELD_VOLUME_TEMPLATE_ID = "volumeTemplateId";
    public static final String FIELD_DEPLOYMENT_UNIT_ID = "deploymentUnitId";
    public static final String FIELD_SERVICE_INDEX_STRATEGY = "serviceIndexStrategy";

    public static final String ACTION_SERVICE_ACTIVATE = "activate";
    public static final String ACTION_SERVICE_CREATE = "create";
    public static final String ACTION_SERVICE_ADD_SERVICE_LINK = "addservicelink";
    public static final String ACTION_SERVICE_REMOVE_SERVICE_LINK = "removeservicelink";
    public static final String ACTION_STACK_ACTIVATE_SERVICES = "activateservices";
    public static final String ACTION_STACK_DEACTIVATE_SERVICES = "deactivateservices";
    public static final String ACTION_SERVICE_SET_SERVICE_LINKS = "setservicelinks";
    public static final String ACTION_SERVICE_UPGRADE = "upgrade";
    public static final String ACTION_SERVICE_ROLLBACK = "rollback";
    public static final String ACTION_ADD_OUTPUTS = "addoutputs";
    public static final String ACTION_SERVICE_RESTART = "restart";
    public static final String ACTION_SERVICE_CERTIFICATE = "certificate";

    public static final String PROCESS_SERVICE_ACTIVATE = "service." + ACTION_SERVICE_ACTIVATE;
    public static final String PROCESS_SERVICE_CREATE = "service." + ACTION_SERVICE_CREATE;
    public static final String PROCESS_STACK_ACTIVATE_SERVICES = "stack." + ACTION_STACK_ACTIVATE_SERVICES;
    public static final String PROCESS_STACK_DEACTIVATE_SERVICES = "stack." + ACTION_STACK_DEACTIVATE_SERVICES;
    public static final String PROCESS_STACK_UPDATE = "stack.update";
    public static final String PROCESS_STACK_REMOVE = "stack.remove";
    public static final String PROCESS_SERVICE_DEACTIVATE = "service.deactivate";
    public static final String PROCESS_SERVICE_REMOVE = "service.remove";
    public static final String PROCESS_STACK_EXPORT_CONFIG = "stack.exportconfig";
    public static final String PROCESS_SERVICE_ADD_SERVICE_LINK = "service." + ACTION_SERVICE_ADD_SERVICE_LINK;
    public static final String PROCESS_SERVICE_REMOVE_SERVICE_LINK = "service." + ACTION_SERVICE_REMOVE_SERVICE_LINK;
    public static final String PROCESS_SERVICE_CONSUME_MAP_CREATE = "serviceconsumemap.create";
    public static final String PROCESS_SERVICE_CONSUME_MAP_REMOVE = "serviceconsumemap.remove";
    public static final String PROCESS_SERVICE_CONSUME_MAP_UPDATE = "serviceconsumemap.update";
    public static final String PROCESS_SERVICE_UPDATE = "service.update";
    public static final String PROCESS_SERVICE_SET_SERVICE_LINKS = "service." + ACTION_SERVICE_SET_SERVICE_LINKS;
    public static final String PROCESS_SERVICE_EXPOSE_MAP_CREATE = "serviceexposemap.create";
    public static final String PROCESS_SERVICE_EXPOSE_MAP_REMOVE = "serviceexposemap.remove";
    public static final String PROCESS_SERVICE_UPGRADE = "service." + ACTION_SERVICE_UPGRADE;
    public static final String PROCESS_SERVICE_ROLLBACK = "service." + ACTION_SERVICE_ROLLBACK;
    public static final String PROCESS_SERVICE_FINISH_UPGRADE = "service.finishupgrade";
    public static final String PROCESS_SERVICE_RESTART = "service." + ACTION_SERVICE_RESTART;
    public static final String PROCESS_SERVICE_INDEX_REMOVE = "serviceindex.remove";
    public static final String PROCESS_SERVICE_CERTIFICATE = "service." + ACTION_SERVICE_CERTIFICATE;

    public static final String LINK_DOCKER_COMPOSE_CONFIG = "dockerComposeConfig";
    public static final String LINK_RANCHER_COMPOSE_CONFIG = "rancherComposeConfig";
    public static final String LINK_COMPOSE_CONFIG = "composeConfig";

    public static final String LABEL_SERVICE_DEPLOYMENT_UNIT = "io.rancher.service.deployment.unit";
    public static final String LABEL_STACK_NAME = "io.rancher.stack.name";
    public static final String LABEL_STACK_SERVICE_NAME = "io.rancher.stack_service.name";
    // LEGACY: preserving project_name
    public static final String LABEL_PROJECT_NAME = "io.rancher.project.name";
    public static final String LABEL_PROJECT_SERVICE_NAME = "io.rancher.project_service.name";
    public static final String LABEL_SERVICE_GLOBAL = "io.rancher.scheduler.global";
    public static final String LABEL_SERVICE_REQUESTED_HOST_ID = "io.rancher.service.requested.host.id";
    public static final String LABEL_SERVICE_LAUNCH_CONFIG = "io.rancher.service.launch.config";
    public static final String LABEL_SIDEKICK = "io.rancher.sidekicks";
    public static final String LABEL_LB_TARGET = "io.rancher.loadbalancer.target.";
    public static final String LABEL_OVERRIDE_HOSTNAME = "io.rancher.container.hostname_override";
    public static final String LABEL_LB_SSL_PORTS = "io.rancher.loadbalancer.ssl.ports";
    public static final String LABEL_LB_PROXY_PORTS = "io.rancher.loadbalancer.proxy-protocol.ports";

    public static final String LABEL_SELECTOR_CONTAINER = "io.rancher.service.selector.container";
    public static final String LABEL_SELECTOR_LINK = "io.rancher.service.selector.link";
    public static final String LABEL_SERVICE_HASH = "io.rancher.service.hash";

    public static final String PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";

    public static final String STATE_UPGRADING = "upgrading";
    public static final String STATE_ROLLINGBACK = "rolling-back";
    public static final String STATE_CANCELING_UPGRADE = "canceling-upgrade";
    public static final String STATE_CANCELED_UPGRADE = "canceled-upgrade";
    public static final String STATE_UPGRADED = "upgraded";
    public static final String STATE_FINISHING_UPGRADE = "finishing-upgrade";
    public static final String STATE_RESTARTING = "restarting";

    public static final String IMAGE_NONE = "rancher/none";

    public static final String AUDIT_LOG_REMOVE_EXTRA = "Removing extra service instance";
    public static final String AUDIT_LOG_REMOVE_UNHEATLHY = "Removing unhealthy service instance";
    public static final String AUDIT_LOG_REMOVE_BAD = "Removing bad service instance";
    public static final String AUDIT_LOG_CREATE_EXTRA = "Creating extra service instance";

    public static final String SERVICE_INDEX_DU_STRATEGY = "deploymentUnitBased";

    public static final String PROCESS_DATA_SERVICE_RECONCILE = "reconcileState";

    public static boolean isSystem(Stack stack) {
        return stack.getSystem() || DataAccessor.fieldBool(stack, FIELD_SYSTEM)|| DataAccessor.fieldBool(stack, "isSystem");
    }

    public static boolean isSystem(Service service) {
        return service.getSystem() || DataAccessor.fieldBool(service, FIELD_SYSTEM);
    }
}
