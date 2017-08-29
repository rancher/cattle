package io.cattle.platform.core.constants;

import io.cattle.platform.util.type.CollectionUtils;

import java.util.Set;

public class NetworkConstants {

    public static final String INTERNAL_DNS_SEARCH_DOMAIN = "rancher.internal";

    public static final String FIELD_DEFAULT_POLICY_ACTION = "defaultPolicyAction";
    public static final String FIELD_DNS = "dns";
    public static final String FIELD_DNS_SEARCH = "dnsSearch";
    public static final String FIELD_HOST_PORTS = "hostPorts";
    public static final String FIELD_MAC_PREFIX = "macPrefix";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_POLICY = "policy";
    public static final String FIELD_SUBNETS = "subnets";

    public static final String KIND_CNI = "cni";

    public static final String KIND_DOCKER_HOST = "host";
    public static final String KIND_DOCKER_NONE = "none";
    public static final String KIND_DOCKER_CONTAINER = "container";
    public static final String KIND_DOCKER_BRIDGE = "dockerBridge";

    public static final Set<String> NETWORK_BUILTIN = CollectionUtils.set(
        KIND_DOCKER_HOST,
        KIND_DOCKER_NONE,
        KIND_DOCKER_CONTAINER,
        KIND_DOCKER_BRIDGE
    );

    public static final String NETWORK_MODE_HOST = "host";
    public static final String NETWORK_MODE_NONE = "none";
    public static final String NETWORK_MODE_DEFAULT = "default";
    public static final String NETWORK_MODE_BRIDGE = "bridge";
    public static final String NETWORK_MODE_CONTAINER = "container";
    public static final String NETWORK_MODE_MANAGED = "managed";

}
