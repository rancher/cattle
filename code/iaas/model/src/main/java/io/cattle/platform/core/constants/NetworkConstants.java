package io.cattle.platform.core.constants;

public class NetworkConstants {

    public static final String INTERNAL_DNS_SEARCH_DOMAIN = "rancher.internal";

    public static final String FIELD_MAC_PREFIX = "macPrefix";
    public static final String FIELD_CIDR = "cidr";
    public static final String FIELD_SERVICES_DOMAIN = "servicesDomain";
    public static final String FIELD_SUBNETS = "subnets";
    public static final String FIELD_DNS = "dns";
    public static final String FIELD_DNS_SEARCH = "dnsSearch";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_HOST_PORTS = "hostPorts";

    public static final String KIND_CNI = "cni";
    public static final String KIND_NETWORK = "network";
    public static final String KIND_VIP_NETWORK = "vipNetwork";

    public static final String KIND_DOCKER_HOST = "dockerHost";
    public static final String KIND_DOCKER_NONE = "dockerNone";
    public static final String KIND_DOCKER_CONTAINER = "dockerContainer";
    public static final String KIND_DOCKER_BRIDGE = "dockerBridge";

    public static final String PREFIX_KIND_DOCKER = "docker";

    public static final String NETWORK_MODE_HOST = "host";
    public static final String NETWORK_MODE_NONE = "none";
    public static final String NETWORK_MODE_DEFAULT = "default";
    public static final String NETWORK_MODE_BRIDGE = "bridge";
    public static final String NETWORK_MODE_CONTAINER = "container";
    public static final String NETWORK_MODE_MANAGED = "managed";

}
