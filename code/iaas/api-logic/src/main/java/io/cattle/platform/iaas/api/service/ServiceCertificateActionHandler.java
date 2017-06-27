package io.cattle.platform.iaas.api.service;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.credential.ApiKeyCertificateDownloadLinkHandler;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceCertificateActionHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceCertificateActionHandler.class);

    CertificateService certService;

    public ServiceCertificateActionHandler(CertificateService certService) {
        super();
        this.certService = certService;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }

        Service service = (Service)obj;

        String serviceName = service.getName();
        try {
            String certs = certService.getServiceCertificate(service);
            if (certs == null) {
                return null;
            }
            ApiKeyCertificateDownloadLinkHandler.prepareRequest(serviceName + "-certs.zip", request);
            request.getOutputStream().write(Base64.decodeBase64(certs));
        } catch (Exception e) {
            log.error("Failed to generate certificate for service [{}]", service.getId(), e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "CertGenerationFailed");
        }

        return new Object();
    }

}