package io.cattle.platform.servicediscovery.api.service;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.util.List;
import java.util.Map;

public interface ServiceDiscoveryApiService {
    void addServiceLink(Service service, ServiceLink serviceLink);

    void removeServiceLink(Service service, ServiceLink serviceLink);

    List<? extends Service> listStackServices(long stackId);

    Map.Entry<String, String> buildComposeConfig(List<? extends Service> services, Stack stack);

    String buildDockerComposeConfig(List<? extends Service> services, Stack stack);

    String buildRancherComposeConfig(List<? extends Service> services);

    String getServiceCertificate(Service service);

    boolean isV1LB(Service service);
}
