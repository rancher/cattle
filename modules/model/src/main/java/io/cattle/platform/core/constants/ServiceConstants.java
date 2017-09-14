package io.cattle.platform.core.constants;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class ServiceConstants {

    public static final String DEFAULT_STACK_NAME = "Default";
    public static final String KIND_SERVICE = "service";
    public static final String KIND_LOAD_BALANCER_SERVICE = "loadBalancerService";
    public static final String KIND_EXTERNAL_SERVICE = "externalService";
    public static final String KIND_DNS_SERVICE = "dnsService";
    public static final String KIND_STORAGE_DRIVER_SERVICE = "storageDriverService";
    public static final String KIND_NETWORK_DRIVER_SERVICE = "networkDriverService";
    public static final String KIND_SCALING_GROUP_SERVICE = "scalingGroup";
    public static final String KIND_SELECTOR_SERVICE = "selectorService";
    public static final String KIND_LAUNCH_CONFIG = "launchConfig";

    public static final String KIND_DEPLOYMENT_UNIT = "deploymentUnit";

    public static final String TYPE_STACK = "stack";

    public static final String FIELD_BATCHSIZE = "batchSize";
    public static final String FIELD_BLKIOOPTIONS = "blkioDeviceOptions";
    public static final String FIELD_COMPLETE_UPDATE = "completeUpdate";
    public static final String FIELD_CREATE_ONLY = "createOnly";
    public static final String FIELD_EXTERNALIPS = "externalIpAddresses";
    public static final String FIELD_FORCE_UPGRADE = "forceUpgrade";
    public static final String FIELD_FQDN = "fqdn";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_INSTANCE_IDS = "instanceIds";
    public static final String FIELD_INTERVAL_MILLISEC = "intervalMillis";
    public static final String FIELD_LAUNCH_CONFIG = "launchConfig";
    public static final String FIELD_LB_CONFIG = "lbConfig";
    public static final String FIELD_LOG_CONFIG = "logConfig";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_NETWORK_DRIVER = "networkDriver";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_PORT_RULES = "portRules";
    public static final String FIELD_PUBLIC_ENDPOINTS = "publicEndpoints";
    public static final String FIELD_RESTART_TRIGGER = "restartTrigger";
    public static final String FIELD_SCALE_INCREMENT = "scaleIncrement";
    public static final String FIELD_SCALE_MAX = "scaleMax";
    public static final String FIELD_SCALE_MIN = "scaleMin";
    public static final String FIELD_SCALE = "scale";
    public static final String FIELD_SECONDARY_LAUNCH_CONFIGS = "secondaryLaunchConfigs";
    public static final String FIELD_SELECTOR_CONTAINER = "selector";
    public static final String FIELD_SERVICE_IDS = "serviceIds";
    public static final String FIELD_SERVICE_LINKS = "serviceLinks";
    public static final String FIELD_START_FIRST_ON_UPGRADE = "startFirst";
    public static final String FIELD_STORAGE_DRIVER = "storageDriver";
    public static final String FIELD_TMPFS= "tmpfs";
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_ULIMITS = "ulimits";
    public static final String FIELD_UPGRADE_LAST_RUN = "upgradeLastRun";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_VOLUME_DRIVER = "driver";
    public static final String FIELD_VOLUME_DRIVER_OPTS = "driverOpts";
    public static final String FIELD_VOLUME_EXTERNAL = "external";
    public static final String FIELD_VOLUME_PER_CONTAINER = "perContainer";

    public static final String ACTION_SERVICE_ACTIVATE = "activate";
    public static final String ACTION_STACK_DEACTIVATE_SERVICES = "deactivateservices";
    public static final String ACTION_SERVICE_UPGRADE = "upgrade";
    public static final String ACTION_SERVICE_ROLLBACK = "rollback";
    public static final String ACTION_SERVICE_RESTART = "restart";

    public static final String PROCESS_SERVICE_ACTIVATE = "service." + ACTION_SERVICE_ACTIVATE;
    public static final String PROCESS_STACK_FINISH_UPGRADE = "stack.finishupgrade";
    public static final String PROCESS_DU_CREATE = "deploymentunit.create";
    public static final String PROCESS_DU_ACTIVATE = "deploymentunit.activate";
    public static final String PROCESS_DU_PAUSE = "deploymentunit.pause";
    public static final String PROCESS_SERVICE_PAUSE = "service.pause";

    public static final String LINK_COMPOSE_CONFIG = "composeConfig";

    public static final String PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";

    public static final String STATE_UPGRADING = "upgrading";
    public static final String STATE_ROLLINGBACK = "rolling-back";
    public static final String STATE_PAUSING = "pausing";
    public static final String STATE_PAUSED = "paused";
    public static final String STATE_UPGRADED = "upgraded";
    public static final String STATE_FINISHING_UPGRADE = "finishing-upgrade";
    public static final String STATE_RESTARTING = "restarting";

    public static final String IMAGE_NONE = "rancher/none";
    public final static String IMAGE_DNS = "rancher/dns-service";

    public static final List<String> SERVICE_INSTANCE_NAME_DIVIDORS = Arrays.asList("-", "_");

    public static final String[] NS_DEPS = new String[] {
            InstanceConstants.FIELD_IPC_CONTAINER_ID,
            InstanceConstants.FIELD_PID_CONTAINER_ID,
            InstanceConstants.FIELD_NETWORK_CONTAINER_ID,
            InstanceConstants.FIELD_VOLUMES_FROM
    };

    public static String getServiceIndexFromInstanceName(String instanceName) {
        for (String divider : SERVICE_INSTANCE_NAME_DIVIDORS) {
            if (!instanceName.contains(divider)) {
                continue;
            }
            String serviceSuffix = instanceName.substring(instanceName.lastIndexOf(divider) + 1);
            if (!StringUtils.isEmpty(serviceSuffix) && serviceSuffix.matches("\\d+")) {
                return serviceSuffix;
            }
        }
        return "";
    }
}
