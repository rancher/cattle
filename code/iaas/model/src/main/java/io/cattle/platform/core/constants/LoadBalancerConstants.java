package io.cattle.platform.core.constants;

public class LoadBalancerConstants {
    public static final String OBJECT_LB_LISTENER = "loadbalancerlistener";
    public static final String OBJECT_LB_CONFIG = "loadbalancerconfig";
    public static final String OBJECT_LB = "loadbalancer";

    public static final String FIELD_LB_LISTENER_IDS = "loadBalancerListenerIds";
    public static final String FIELD_LB_LISTENER_ID = "loadBalancerListenerId";
    public static final String FIELD_LB_ID = "loadBalancerId";
    public static final String FIELD_WEIGHT = "weight";
    public static final String FIELD_LB_TARGET_INSTANCE_ID = "instanceId";
    public static final String FIELD_LB_TARGET_IPADDRESS = "ipAddress";
    public static final String FIELD_LB_TARGET_INSTANCE_IDS = "instanceIds";
    public static final String FIELD_LB_TARGET_IPADDRESSES = "ipAddresses";
    public static final String FIELD_LB_HOST_ID = "hostId";
    public static final String FIELD_LB_HOST_IDS = "hostIds";
    public static final String FIELD_LB_INSTANCE_IMAGE_UUID = "loadBalancerInstanceImageUuid";
    public static final String FIELD_LB_INSTANCE_URI_PREDICATE = "loadBalancerInstanceUriPredicate";
    public static final String FIELD_LB_NETWORK_ID = "networkId";
    public static final String FIELD_LB_SOURCE_PORT = "sourcePort";
    public static final String FIELD_LB_HEALTH_CHECK = "healthCheck";
    public static final String FIELD_LB_APP_COOKIE_POLICY = "appCookieStickinessPolicy";
    public static final String FIELD_LB_COOKIE_POLICY = "lbCookieStickinessPolicy";

    public static final String ACTION_ADD_HOST = "addhost";
    public static final String ACTION_REMOVE_HOST = "removehost";

    public static final String ACTION_LB_CONFIG_ADD_LISTENER = "addlistener";
    public static final String ACTION_LB_CONFIG_REMOVE_LISTENER = "removelistener";
    public static final String ACTION_LB_ADD_TARGET = "addtarget";
    public static final String ACTION_LB_REMOVE_TARGET = "removetarget";

    public static final String PROCESS_LB_CONFIG_LISTENER_MAP_CREATE = "loadbalancerconfiglistenermap.create";
    public static final String PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE = "loadbalancerconfiglistenermap.remove";
    public static final String PROCESS_LB_CREATE = "loadbalancer.create";
    public static final String PROCESS_LB_UPDATE = "loadbalancer.update";
    public static final String PROCESS_LB_SET_TARGETS = "loadbalancer.settargets";
    public static final String PROCESS_LB_CONFIG_ADD_LISTENER = "loadbalancerconfig." + ACTION_LB_CONFIG_ADD_LISTENER;
    public static final String PROCESS_LB_CONFIG_REMOVE_LISTENER = "loadbalancerconfig."
            + ACTION_LB_CONFIG_REMOVE_LISTENER;
    public static final String PROCESS_LB_CONFIG_SET_LISTENERS = "loadbalancerconfig.setlisteners";
    public static final String PROCESS_LB_ADD_HOST = "loadbalancer." + ACTION_ADD_HOST;
    public static final String PROCESS_LB_REMOVE_HOST = "loadbalancer." + ACTION_REMOVE_HOST;
    public static final String PROCESS_LB_SET_HOSTS = "loadbalancer.sethosts";
    public static final String PROCESS_LB_ADD_TARGET = "loadbalancer." + ACTION_LB_ADD_TARGET;
    public static final String PROCESS_LB_REMOVE_TARGET = "loadbalancer." + ACTION_LB_REMOVE_TARGET;
    public static final String PROCESS_LB_TARGET_MAP_CREATE = "loadbalancertarget.create";
    public static final String PROCESS_LB_TARGET_MAP_REMOVE = "loadbalancertarget.remove";
    public static final String PROCESS_LB_LISTENER_REMOVE = "loadbalancerlistener.remove";
    public static final String PROCESS_LB_HOST_MAP_CREATE = "loadbalancerhostmap.create";
    public static final String PROCESS_LB_HOST_MAP_REMOVE = "loadbalancerhostmap.remove";
    public static final String PROCESS_LB_REMOVE = "loadbalancer.remove";
    public static final String PROCESS_LB_CONFIG_REMOVE = "loadbalancerconfig.remove";
    public static final String PROCESS_GLB_ADD_LB = "globalloadbalancer.addloadbalancer";
    public static final String PROCESS_GLB_REMOVE_LB = "globalloadbalancer.removeloadbalancer";
}
