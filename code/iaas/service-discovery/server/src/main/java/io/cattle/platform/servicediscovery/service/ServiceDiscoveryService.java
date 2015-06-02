package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

public interface ServiceDiscoveryService {

    SimpleEntry<String, String> buildComposeConfig(List<? extends Service> services);

    int[] getWeights(int size, int total);

    String buildDockerComposeConfig(List<? extends Service> services);

    String buildRancherComposeConfig(List<? extends Service> services);

    void cleanupLoadBalancerService(Service service);

    String generateServiceInstanceName(Service service, String launchConfigName, int finalOrder);

    void createLoadBalancerService(Service service);

    void removeServiceMaps(Service service);

    Map<String, String> getServiceLabels(Service service);

    List<? extends Service> listEnvironmentServices(long environmentId);

    Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams, String launchConfigName);

    List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName);

    List<Service> getServicesFor(Object obj);

    List<String> getServiceLaunchConfigNames(Service service);

    Object getLaunchConfigObject(Service service, String launchConfigName, String objectName);

}
