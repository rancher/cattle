package io.cattle.platform.certificate;

import io.cattle.platform.core.model.Service;

public interface CertificateService {

    String getServiceCertificate(Service service);

}
