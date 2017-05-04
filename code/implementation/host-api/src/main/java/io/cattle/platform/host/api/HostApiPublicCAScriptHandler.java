package io.cattle.platform.host.api;

import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.security.cert.Certificate;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

public class HostApiPublicCAScriptHandler implements ScriptsHandler {

    public static final String FILENAME = "ca.pem";

    @Inject
    RSAKeyProvider rsaKeyProvider;

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        String id = request.getId();

        if (!FILENAME.equals(id)) {
            return false;
        }

        Certificate cert = rsaKeyProvider.getCACertificate();
        byte[] content = rsaKeyProvider.toBytes(cert);

        HttpServletResponse response = request.getServletContext().getResponse();

        response.setContentLength(content.length);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + FILENAME);
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "private");
        response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");

        response.getOutputStream().write(content);

        return true;
    }

}