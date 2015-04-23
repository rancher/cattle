package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceDiscoveryService {

    SimpleEntry<String, String> buildConfig(List<? extends Service> services);

    Map<String, Object> buildLaunchData(Service service);

    int[] getWeights(int size, int total);

    long getServiceNetworkId(Service service);

    String buildDockerComposeConfig(List<? extends Service> services);

    String buildRancherComposeConfig(List<? extends Service> services);

    String getLoadBalancerName(Service service);

    Pair<Long, Long> getInstanceToServicePair(Instance instance);

    void activateService(Service service, int scale);

    void activateLoadBalancerService(Service service, int scale);

    void deactivateLoadBalancerService(Service service);

    void deactivateService(Service service);

    void scaleDownService(Service service, int requestedScale);

    void scaleDownLoadBalancerService(Service service, int requestedScale);

}
