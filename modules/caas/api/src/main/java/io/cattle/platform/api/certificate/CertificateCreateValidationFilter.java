package io.cattle.platform.api.certificate;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.ssh.common.SslCertificateUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CertificateCreateValidationFilter extends AbstractValidationFilter {
    private static final Logger log = LoggerFactory.getLogger(CertificateCreateValidationFilter.class);

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String cert = DataAccessor.getFieldFromRequest(request, "cert", String.class);

        Certificate certificate = request.proxyRequestObject(Certificate.class);
        setCertificateFields(cert, certificate);

        return super.create(type, request, next);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        String cert = DataAccessor.getFieldFromRequest(request, "cert", String.class);

        Certificate certificate = request.proxyRequestObject(Certificate.class);
        setCertificateFields(cert, certificate);

        return super.update(type, id, request, next);
    }

    protected void setCertificateFields(String cert, Certificate certificate) {
        try {
            Map<String, Object> fields = DataAccessor.getWritableFields(certificate);
            fields.put("certFingerprint", SslCertificateUtils.getCertificateFingerprint(cert));
            fields.put("expiresAt", SslCertificateUtils.getExpirationDate(cert));
            fields.put("CN", SslCertificateUtils.getCN(cert));
            fields.put("issuer", SslCertificateUtils.getIssuer(cert));
            fields.put("issuedAt", SslCertificateUtils.getIssuedDate(cert));
            fields.put("version", SslCertificateUtils.getVersion(cert));
            fields.put("algorithm", SslCertificateUtils.getAlgorithm(cert));
            fields.put("serialNumber", SslCertificateUtils.getSerialNumber(cert));
            fields.put("keySize", SslCertificateUtils.getKeySize(cert));
            fields.put("subjectAlternativeNames", SslCertificateUtils.getSubjectAlternativeNames(cert));
        } catch (Exception e) {
            String className = e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass()
                    .getSimpleName();
            log.info("Exception parsing certificate fields: {} : [{}]", className, e.getMessage());
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_FORMAT,
                    e.getMessage(), null);
        }
    }
}
