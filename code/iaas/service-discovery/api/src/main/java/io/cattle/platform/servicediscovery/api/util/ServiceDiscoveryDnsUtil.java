package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;

import org.apache.commons.lang3.StringUtils;

public class ServiceDiscoveryDnsUtil {
    public static final String RANCHER_NAMESPACE = "rancher.internal";
    public static final String KUBERNETES_SVC_NAMESPACE = "svc.cluster.local";
    public static final String METADATA_FQDN = "rancher-metadata" + "." + RANCHER_NAMESPACE + ".";
    public static final String METADATA_IP = "169.254.169.250";

    private static String getNamespace(Service service) {
        if (service.getKind().equalsIgnoreCase("kubernetesservice")) {
            return KUBERNETES_SVC_NAMESPACE;
        }
        return RANCHER_NAMESPACE;
    }

    public static String getServiceNamespace(Environment stack, Service service, String launchConfigName) {
        return new StringBuilder().append(launchConfigName).append(".").append(getStackNamespace(stack, service))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(Environment stack, Service service) {
        return new StringBuilder().append(stack.getName()).append(".")
                .append(getNamespace(service)).toString().toLowerCase();
    }


    public static String getFqdn(Environment stack, Service service, String launchConfigName, boolean forDefault) {
        if (forDefault) {
            return getServiceNamespace(stack, service, launchConfigName).toLowerCase() + ".";

        } else {
            return launchConfigName.toLowerCase() + ".";
        }
    }

    public static String getDnsName(Service service, Environment stack, String linkName,
            String dnsPrefix, boolean self, boolean forDefault) {

        if (!StringUtils.isEmpty(linkName)) {
            return linkName;
        }

        String dnsName = null;
        if (self) {
            dnsName = dnsPrefix == null ? service.getName() : dnsPrefix;
        } else {
            dnsName = dnsPrefix == null ? service.getName() : dnsPrefix + "." + service.getName();
        }

        return ServiceDiscoveryDnsUtil.getFqdn(stack, service, dnsName, forDefault);
    }
}
