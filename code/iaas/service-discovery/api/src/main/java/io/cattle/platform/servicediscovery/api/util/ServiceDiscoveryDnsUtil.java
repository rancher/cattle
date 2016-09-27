package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

public class ServiceDiscoveryDnsUtil {

    public static String getGlobalNamespace(Service service) {
        return NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN;
    }

    public static String getServiceNamespace(Stack stack, Service service) {
        return new StringBuilder().append(service.getName()).append(".").append(getStackNamespace(stack, service))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(Stack stack, Service service) {
        return new StringBuilder().append(stack.getName()).append(".")
                .append(getGlobalNamespace(service)).toString().toLowerCase();
    }
}
