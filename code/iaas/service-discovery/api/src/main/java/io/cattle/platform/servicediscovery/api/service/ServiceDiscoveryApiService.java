package io.cattle.platform.servicediscovery.api.service;

import io.cattle.platform.core.model.Service;

public interface ServiceDiscoveryApiService {

    String getServiceCertificate(Service service);
}
