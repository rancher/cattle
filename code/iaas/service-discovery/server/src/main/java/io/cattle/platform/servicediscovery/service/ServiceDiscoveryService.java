package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

import java.util.List;

public interface ServiceDiscoveryService {

    void cleanupLoadBalancerService(Service service);

    void createLoadBalancerService(Service service, List<? extends Long> certIds, Long defaultCertId);

    void removeServiceMaps(Service service);

    List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName);

    boolean isActiveService(Service service);

    void addToLoadBalancerService(Service lbSvc, ServiceExposeMap serviceInstanceToAdd);

    void cloneConsumingServices(Service fromService, Service toService);

    void setVIP(Service service);

    void releaseVip(Service service);

    void updateLoadBalancerService(Service service, List<? extends Long> certIds, Long defaultCertId);

    void addServiceLink(Service service, ServiceLink serviceLink);

    boolean isSelectorLinkMatch(Service sourceService, Service targetService);

    boolean isSelectorContainerMatch(Service sourceService, long instanceId);

    boolean isServiceInstance(Service service, Instance instance);

}
