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
    public static final String FIELD_LB_HOST_ID = "hostId";
    public static final String FIELD_LB_INSTANCE_IMAGE_UUID = "loadBalancerInstanceImageUuid";
    public static final String FIELD_LB_INSTANCE_URI_PREDICATE = "loadBalancerInstanceUriPredicate";

    public static final String ACTION_ADD_HOST = "addhost";
    public static final String ACTION_REMOVE_HOST = "removehost";

    public static final String PROCESS_LB_CONFIG_LISTENER_MAP_CREATE = "loadbalancerconfiglistenermap.create";
    public static final String PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE = "loadbalancerconfiglistenermap.remove";
    public static final String PROCESS_LB_CONFIG_ADD_LISTENER = "loadbalancerconfig.addlistener";
    public static final String PROCESS_LB_CONFIG_REMOVE_LISTENER = "loadbalancerconfig.removelistener";
    public static final String PROCESS_LB_CREATE = "loadbalancer.create";
    public static final String PROCESS_LB_UPDATE = "loadbalancer.update";
    public static final String PROCESS_LB_ADD_TARGET = "loadbalancer.addtarget";
    public static final String PROCESS_LB_REMOVE_TARGET = "loadbalancer.removetarget";
    public static final String PROCESS_LB_ADD_HOST = "loadbalancer." + ACTION_ADD_HOST;
    public static final String PROCESS_LB_REMOVE_HOST = "loadbalancer." + ACTION_REMOVE_HOST;
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
