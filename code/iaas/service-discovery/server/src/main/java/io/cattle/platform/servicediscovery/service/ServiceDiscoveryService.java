package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

public interface ServiceDiscoveryService {

    SimpleEntry<String, String> buildConfig(List<? extends Service> services);

    Map<String, Object> buildLaunchData(Service service);

    int[] getWeights(int size, int total);

    long getServiceNetworkId(Service service);

    String getInstanceName(Service service, int order);

    String buildDockerComposeConfig(List<? extends Service> services);

    String buildRancherComposeConfig(List<? extends Service> services);

}
