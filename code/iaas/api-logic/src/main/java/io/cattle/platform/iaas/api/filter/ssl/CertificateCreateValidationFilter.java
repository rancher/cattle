package io.cattle.platform.iaas.api.filter.ssl;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.ssh.common.SslCertificateValidationUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import org.apache.commons.lang3.StringUtils;

public class CertificateCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Override
    public String[] getTypes() {
        return new String[] { "certificate" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String cert = DataUtils.getFieldFromRequest(request, "cert", String.class);
        String key = DataUtils.getFieldFromRequest(request, "key", String.class);
        String certChain = DataUtils.getFieldFromRequest(request, "certChain", String.class);

        validateCertificate(cert, key, certChain);

        // set fingerprint
        Certificate certificate = request.proxyRequestObject(Certificate.class);
        try {
            DataUtils.getWritableFields(certificate).put("certFingerprint",
                    SslCertificateValidationUtils.getCertificateFingerprint(cert));
        } catch (Exception e) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_FORMAT, "cert");
        }

        return super.create(type, request, next);
    }

    protected void validateCertificate(String cert, String key, String certChain) {
        try {
            if (StringUtils.isEmpty(certChain)) {
                SslCertificateValidationUtils.verifySelfSignedCertificate(cert, key);
            }
        } catch (Exception e) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_FORMAT, "cert");
        }

        try {
            if (!StringUtils.isEmpty(certChain)) {
                SslCertificateValidationUtils.verifyCertificateChain(cert, certChain, key);
            }
        } catch (Exception e) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_FORMAT, "certChain");
        }
    }
}
