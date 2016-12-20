package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.credential.ApiKeyCertificateDownloadLinkHandler;
import io.cattle.platform.servicediscovery.api.service.ServiceDiscoveryApiService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ServiceCertificateActionHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceCertificateActionHandler.class);

    @Inject
    ServiceDiscoveryApiService sdApiService;

    @Override
    public String getName() {
        return ServiceConstants.PROCESS_SERVICE_CERTIFICATE;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }

        Service service = (Service)obj;

        String serviceName = service.getName();
        try {
            String certs = sdApiService.getServiceCertificate(service);
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