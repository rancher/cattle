package io.cattle.platform.api.hostapi;

import io.cattle.platform.api.requesthandler.ScriptsHandler;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.Certificate;

public class HostApiPublicCAScriptHandler implements ScriptsHandler {

    public static final String FILENAME = "ca.pem";

    RSAKeyProvider rsaKeyProvider;

    public HostApiPublicCAScriptHandler(RSAKeyProvider rsaKeyProvider) {
        super();
        this.rsaKeyProvider = rsaKeyProvider;
    }

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