package io.cattle.platform.core.util;

public class SystemLabels {
    public static final String LABEL_AGENT_CREATE = "io.rancher.container.create_agent";
    public static final String LABEL_AGENT_ROLE = "io.rancher.container.agent.role";
    public static final String LABEL_USE_RANCHER_DNS = "io.rancher.container.dns";
    public static final String LABEL_REQUESTED_IP = "io.rancher.container.requested_ip";
    public static final String LABEL_AGENT_URI_PREFIX = "io.rancher.container.agent.uri.prefix";
    public static final String LABEL_VOLUME_CLEANUP_STRATEGY = "io.rancher.container.volume_cleanup_strategy";
    public static final String LABEL_SERVICE_CONTAINER_START_ONCE = "io.rancher.container.start_once";
    public static final String LABEL_CNI_NETWORK = "io.rancher.cni.network";
    public static final String LABEL_RANCHER_UUID = "io.rancher.container.uuid";
    public static final String LABEL_RANCHER_NETWORK = "io.rancher.container.network";
    public static final String LABEL_DISPLAY_NAME = "io.rancher.container.display_name";
    public static final String LABEL_PROXY_PORT = "io.rancher.websocket.proxy.port";
    public static final String LABEL_PROXY_SCHEME = "io.rancher.websocket.proxy.scheme";
    public static final String LABEL_STACK_NAME = "io.rancher.stack.name";
    public static final String LABEL_STACK_SERVICE_NAME = "io.rancher.stack_service.name";
    public static final String PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";
    public static final String CNI_WAIT = "io.rancher.cni.wait";
    public static final String IP_ADDRESS = "io.rancher.container.ip";
    public static final String MAC_ADDRESS = "io.rancher.container.mac_address";

    /**
     * Indicates an instance runs an agent that provides the labels provider service
     */
    public static final String LABEL_AGENT_SERVICE_LABELS_PROVIDER = "io.rancher.container.agent_service.labels_provider";
    public static final String LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER = "io.rancher.container.agent_service.scheduling";
    public static final String LABEL_AGENT_SERVICE_COMPUTE = "io.rancher.container.agent_service.compute";

    public static final String LABEL_VM = "io.rancher.vm";
    public static final String LABEL_VM_USERDATA = "io.rancher.vm.userdata";
    public static final String LABEL_VM_MEMORY = "io.rancher.vm.memory";
    public static final String LABEL_VM_VCPU = "io.rancher.vm.vcpu";
}
