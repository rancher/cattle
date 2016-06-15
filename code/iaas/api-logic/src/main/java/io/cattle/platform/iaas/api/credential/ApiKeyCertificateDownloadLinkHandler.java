package io.cattle.platform.iaas.api.credential;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiKeyCertificateDownloadLinkHandler implements LinkHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyCertificateDownloadLinkHandler.class);

    @Inject
    RSAKeyProvider keyProvider;
    @Inject
    ObjectManager objectManager;

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
            Credential cred = (Credential)obj;
            String publicValue = cred.getPublicValue();
            String secretValue = cred.getSecretValue();
            if (secretValue == null || publicValue == null) {
                return null;
            }

            try {
                String[] sans = new String[0];
                Account account = objectManager.loadResource(Account.class, cred.getAccountId());
                if (account != null && AccountConstants.SERVICE_KIND.equals(account.getKind())) {
                    Set<String> sanSet = new LinkedHashSet<>();
                    URL url = new URL(request.getResponseUrlBase());
                    sanSet.add(url.getHost());

                    url = new URL(ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP));
                    sanSet.add(url.getHost());

                    sanSet.add("IP:127.0.0.1");
                    sanSet.add("localhost");

                    sans = sanSet.toArray(new String[sanSet.size()]);
                }

                CertSet cert = keyProvider.generateCertificate(publicValue, sans);
                prepareRequest(getFilename(cred, request), request);

                HttpServletResponse response = request.getServletContext().getResponse();
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

    public static void prepareRequest(String filename, ApiRequest request) throws IOException {
        HttpServletResponse response = request.getServletContext().getResponse();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "private");
        response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
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
