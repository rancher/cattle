package io.cattle.platform.iaas.api.service;

import io.cattle.platform.core.model.Service;

public interface CertificateService {

    String getServiceCertificate(Service service);
}
