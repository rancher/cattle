package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ServiceDiscoveryDnsUtil {
    public static final String RANCHER_NAMESPACE = "rancher.internal";
    public static final String KUBERNETES_SVC_NAMESPACE = "svc.cluster.local";
    public static final String METADATA_FQDN = "rancher-metadata" + "." + RANCHER_NAMESPACE + ".";
    public static final String NETWORK_AGENT_IP = "169.254.169.250";

    public static String getGlobalNamespace(Service service) {
        if (service.getKind().equalsIgnoreCase("kubernetesservice")) {
            return KUBERNETES_SVC_NAMESPACE;
        }
        return RANCHER_NAMESPACE;
    }

    private static String getLaunchConfigNamespace(Stack stack, Service service, String launchConfigName) {
        return new StringBuilder().append(launchConfigName).append(".").append(getStackNamespace(stack, service))
                .toString().toLowerCase();
    }

    public static String getServiceNamespace(Stack stack, Service service) {
        return new StringBuilder().append(service.getName()).append(".").append(getStackNamespace(stack, service))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(Stack stack, Service service) {
        return new StringBuilder().append(stack.getName()).append(".")
                .append(getGlobalNamespace(service)).toString().toLowerCase();
    }

    public static String getFqdn(Stack stack, Service service, String launchConfigName) {
        return getLaunchConfigNamespace(stack, service, launchConfigName).toLowerCase() + ".";
    }

    public static String getDnsName(Service service, Stack stack, String linkName,
            String dnsPrefix, boolean self) {

        if (!StringUtils.isEmpty(linkName)) {
            return linkName.toLowerCase();
        }

        String dnsName = null;
        if (self) {
            dnsName = dnsPrefix == null ? service.getName() : dnsPrefix;
        } else {
            dnsName = dnsPrefix == null ? service.getName() : dnsPrefix + "." + service.getName();
        }

        return ServiceDiscoveryDnsUtil.getFqdn(stack, service, dnsName);
    }

    public static List<String> getNamespaces(Stack stack, Service service, String launchConfigName) {
        List<String> toReturn = new ArrayList<>();
        toReturn.add(getStackNamespace(stack, service));
        toReturn.add(getGlobalNamespace(service));
        toReturn.add(getServiceNamespace(stack, service));

        return toReturn;
    }
}
