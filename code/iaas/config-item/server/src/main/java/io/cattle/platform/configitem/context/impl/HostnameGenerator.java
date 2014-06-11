package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.object.util.DataAccessor;

import com.netflix.config.DynamicStringProperty;

public class HostnameGenerator {

    private static final DynamicStringProperty DEFAULT_DOMAIN = ArchaiusUtil.getString("default.network.domain");
    private static final DynamicStringProperty DEFAULT_PREFIX = ArchaiusUtil.getString("default.hostname.prefix");

    private static final DynamicStringProperty DEFAULT_LOCAL_DOMAIN = ArchaiusUtil.getString("default.local.network.domain");
    private static final DynamicStringProperty DEFAULT_LOCAL_PREFIX = ArchaiusUtil.getString("default.local.hostname.prefix");

    private static final DynamicStringProperty DEFAULT_SERVICES_DOMAIN = ArchaiusUtil.getString("default.services.domain");

    public static String getDefaultDomain(boolean local) {
        return local ? DEFAULT_LOCAL_DOMAIN.get() : DEFAULT_DOMAIN.get();
    }

    public static String getDefaultPrefix(boolean local) {
        return local ? DEFAULT_LOCAL_PREFIX.get() : DEFAULT_PREFIX.get();
    }

    public static String getDefaultServicesDomain() {
        return DEFAULT_SERVICES_DOMAIN.get();
    }

    public static String getServicesDomain(Network network) {
        String servicesDomain = DataAccessor.fieldString(network, NetworkConstants.FIELD_SERVICES_DOMAIN);
        return servicesDomain == null ? getDefaultServicesDomain() : servicesDomain;
    }

    public static String lookup(boolean local, Instance instance, String address, Network network) {
        return lookup(local, instance.getHostname(), instance.getDomain(), address, network.getDomain());
    }

    public static String lookup(boolean local, String instanceHostname, String instanceDomain, String address, String networkDomain) {
        String hostname = local ? instanceHostname : null;
        String domain = local ? instanceDomain : null;
        if ( domain == null && local ) {
            domain = networkDomain;
        }

        String prefix = getDefaultPrefix(local);

        if ( domain == null ) {
            domain = getDefaultDomain(local);
        }

        if ( hostname == null ) {
            if ( address == null ) {
                return null;
            }

            return String.format("%s%s.%s", prefix, address.replaceAll("[.]", "-"), domain);
        } else {
            return String.format("%s.%s", hostname, domain);
        }
    }

}
