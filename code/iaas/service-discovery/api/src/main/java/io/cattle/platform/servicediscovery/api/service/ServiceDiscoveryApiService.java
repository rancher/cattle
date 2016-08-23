package io.cattle.platform.servicediscovery.api.service;

import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Service;

import java.util.List;
import java.util.Map;

public interface ServiceDiscoveryApiService {
    void addServiceLink(Service service, ServiceLink serviceLink);

    void removeServiceLink(Service service, ServiceLink serviceLink);

    void addLoadBalancerServiceLink(Service service, LoadBalancerServiceLink serviceLink);

    List<? extends Service> listStackServices(long stackId);

    Map.Entry<String, String> buildComposeConfig(List<? extends Service> services);

    String buildDockerComposeConfig(List<? extends Service> services);

    String buildRancherComposeConfig(List<? extends Service> services);

    String getServiceCertificate(Service service);
}
