package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface ServiceDao {

    /**
     * @param hostId
     * @return Collection of Services running on host
     */
    List<? extends Service> getServicesOnHost(long hostId);

    List<? extends Instance> getInstancesWithHealtcheckEnabled(long accountId);

    List<Certificate> getLoadBalancerServiceCertificates(Service lbService);

    Certificate getLoadBalancerServiceDefaultCertificate(Service lbService);

    List<Service> getConsumingLbServices(long serviceId);

    void incrementMetadataRevision(long accountId, Object object);
}
