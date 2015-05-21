package io.cattle.platform.docker.constants;

import io.cattle.platform.core.constants.NetworkConstants;

import java.util.HashMap;
import java.util.Map;

public class DockerNetworkConstants {

    public static final String KIND_DOCKER_HOST = "dockerHost";
    public static final String KIND_DOCKER_NONE = "dockerNone";
    public static final String KIND_DOCKER_CONTAINER = "dockerContainer";
    public static final String KIND_DOCKER_BRIDGE = "dockerBridge";

    public static final String NETWORK_MODE_HOST = "host";
    public static final String NETWORK_MODE_NONE = "none";
    public static final String NETWORK_MODE_BRIDGE = "bridge";
    public static final String NETWORK_MODE_CONTAINER = "container";
    public static final String NETWORK_MODE_MANAGED = "managed";

    public static final Map<String, String> MODE_TO_KIND = new HashMap<>();

    static {
        MODE_TO_KIND.put(NETWORK_MODE_HOST, KIND_DOCKER_HOST);
        MODE_TO_KIND.put(NETWORK_MODE_NONE, KIND_DOCKER_NONE);
        MODE_TO_KIND.put(NETWORK_MODE_BRIDGE, KIND_DOCKER_BRIDGE);
        MODE_TO_KIND.put(NETWORK_MODE_CONTAINER, KIND_DOCKER_CONTAINER);
        MODE_TO_KIND.put(NETWORK_MODE_MANAGED, NetworkConstants.KIND_HOSTONLY);
    }
}
