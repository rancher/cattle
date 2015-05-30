package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceDiscoveryService {

    SimpleEntry<String, String> buildComposeConfig(List<? extends Service> services);

    int[] getWeights(int size, int total);

    String buildDockerComposeConfig(List<? extends Service> services);

    String buildRancherComposeConfig(List<? extends Service> services);

    Pair<Long, Long> getInstanceToServicePair(Instance instance);

    void cleanupLoadBalancerService(Service service);

    String generateServiceInstanceName(Service service, int finalOrder);

    void createLoadBalancerService(Service service);

    void removeServiceMaps(Service service);

    Map<String, String> getServiceLabels(Service service);

    List<? extends Service> listEnvironmentServices(long environmentId);

    Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams);

    List<Long> getServiceNetworkIds(Service service);

    List<Integer> getServiceInstanceUsedOrderIds(Service service);

}
