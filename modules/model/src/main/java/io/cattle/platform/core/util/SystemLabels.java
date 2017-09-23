package io.cattle.platform.core.util;

public class SystemLabels {
    public static final String LABEL_AGENT_CREATE = "io.rancher.container.create_agent";
    public static final String LABEL_AGENT_ROLE = "io.rancher.container.agent.role";
    public static final String LABEL_AGENT_URI_PREFIX = "io.rancher.container.agent.uri.prefix";
    public static final String LABEL_CONTAINER_NAME = "io.rancher.container.name";
    public static final String LABEL_CNI_NETWORK = "io.rancher.cni.network";
    public static final String LABEL_CNI_WAIT = "io.rancher.cni.wait";
    public static final String LABEL_DISPLAY_NAME = "io.rancher.container.display_name";
    public static final String LABEL_IP_ADDRESS = "io.rancher.container.ip";
    public static final String LABEL_MAC_ADDRESS = "io.rancher.container.mac_address";
    public static final String LABEL_ORCHESTRATION = "io.rancher.container.orchestration";
    public static final String LABEL_OVERRIDE_HOSTNAME = "io.rancher.container.hostname_override";
    public static final String LABEL_PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";
    public static final String LABEL_PROXY_PORT = "io.rancher.websocket.proxy.port";
    public static final String LABEL_PROXY_SCHEME = "io.rancher.websocket.proxy.scheme";
    public static final String LABEL_RANCHER_NETWORK = "io.rancher.container.network";
    public static final String LABEL_RANCHER_UUID = "io.rancher.container.uuid";
    public static final String LABEL_REQUESTED_IP = "io.rancher.container.requested_ip";
    public static final String LABEL_SELECTOR_CONTAINER = "io.rancher.service.selector.container";
    public static final String LABEL_SERVICE_CONTAINER_START_ONCE = "io.rancher.container.start_once";
    public static final String LABEL_SERVICE_DEPLOYMENT_UNIT = "io.rancher.service.deployment.unit";
    public static final String LABEL_SERVICE_GLOBAL = "io.rancher.scheduler.global";
    public static final String LABEL_SERVICE_LAUNCH_CONFIG = "io.rancher.service.launch.config";
    public static final String LABEL_SERVICE_REQUESTED_HOST_ID = "io.rancher.service.requested.host.id";
    public static final String LABEL_SIDEKICK = "io.rancher.sidekicks";
    public static final String LABEL_STACK_NAME = "io.rancher.stack.name";
    public static final String LABEL_STACK_SERVICE_NAME = "io.rancher.stack_service.name";
    public static final String LABEL_USE_RANCHER_DNS = "io.rancher.container.dns";
    public static final String LABEL_VOLUME_CLEANUP_STRATEGY = "io.rancher.container.volume_cleanup_strategy";
    public static final String LABEL_DNS_SEARCH = "io.rancher.container.dnssearch";

    // K8s labels
    public static final String LABEL_K8S_POD_UID = "io.kubernetes.pod.uid";
    public static final String LABEL_K8S_POD_NAME = "io.kubernetes.pod.name";
    public static final String LABEL_K8S_POD_NAMESPACE = "io.kubernetes.pod.namespace";
    public static final String LABEL_K8S_CONTAINER_NAME = "io.kubernetes.container.name";

    public static final String POD_VALUE = "POD";

    /**
     * Indicates an instance runs an agent that provides the labels provider service
     */
    public static final String LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER = "io.rancher.container.agent_service.scheduling";
    public static final String LABEL_AGENT_SERVICE_METADATA = "io.rancher.container.agent_service.metadata";

    public static final String LABEL_VM = "io.rancher.vm";
    public static final String LABEL_VM_USERDATA = "io.rancher.vm.userdata";
    public static final String LABEL_VM_MEMORY = "io.rancher.vm.memory";
    public static final String LABEL_VM_VCPU = "io.rancher.vm.vcpu";
}
