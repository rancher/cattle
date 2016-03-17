package io.cattle.platform.iaas.api.credential;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiKeyCertificateDownloadLinkHandler implements LinkHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyCertificateDownloadLinkHandler.class);

    @Inject
    RSAKeyProvider keyProvider;

    @Override
    public String[] getTypes() {
        return new String[] { "apiKey", CredentialConstants.TYPE };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return CredentialConstants.LINK_CERTIFICATE.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (obj instanceof Credential) {
            String publicValue = ((Credential) obj).getPublicValue();
            String secretValue = ((Credential) obj).getSecretValue();
            if (secretValue == null || publicValue == null) {
                return null;
            }

            HttpServletResponse response = request.getServletContext().getResponse();

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + getFilename((Credential) obj, request));
            response.setHeader("Cache-Control", "private");
            response.setHeader("Pragma", "private");
            response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");

            try {
                CertSet cert = keyProvider.generateCertificate(publicValue);
                cert.writeZip(response.getOutputStream());
            } catch (Exception e) {
                log.error("Failed to create certificate",  e);
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "CertCreationError", "Failed to create certificate",  e.getMessage());
            }

            return new Object();
        }

        return null;
    }

    protected String getFilename(Credential cred, ApiRequest request) {
        String name = cred.getName();

        if (StringUtils.isBlank(name)) {
            IdFormatter formatter = ApiContext.getContext().getIdFormatter();
            Object id = formatter.formatId(cred.getKind(), cred.getId());

            return "certificate-" + id + ".zip";
        } else {
            return name.toLowerCase().trim().replaceAll("[^a-zA-Z0-9]", "_") + ".zip";
        }
    }

}
