package io.cattle.platform.iaas.api.credential;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

public class SshKeyPemDownloadLinkHandler implements LinkHandler {

    @Override
    public String[] getTypes() {
        return new String[] { "sshKey" };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return CredentialConstants.LINK_PEM_FILE.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (obj instanceof Credential) {
            String secretValue = ((Credential) obj).getSecretValue();
            if (secretValue == null) {
                return null;
            }

            byte[] content = secretValue.getBytes("UTF-8");

            HttpServletResponse response = request.getServletContext().getResponse();

            response.setContentLength(content.length);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + getFilename((Credential) obj, request));
            response.setHeader("Cache-Control", "private");
            response.setHeader("Pragma", "private");
            response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");

            response.getOutputStream().write(content);

            return new Object();
        }

        return null;
    }

    protected String getFilename(Credential cred, ApiRequest request) {
        String name = cred.getName();

        if (StringUtils.isBlank(name)) {
            IdFormatter formatter = ApiContext.getContext().getIdFormatter();
            Object id = formatter.formatId(cred.getKind(), cred.getId());

            return "id_rsa_" + id + ".pem";
        } else {
            return name.trim().replaceAll("[^a-zA-Z0-9]", "_") + ".pem";
        }
    }

}
