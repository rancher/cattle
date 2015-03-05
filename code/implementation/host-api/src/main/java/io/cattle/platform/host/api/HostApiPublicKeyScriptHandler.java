package io.cattle.platform.host.api;

import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostApiPublicKeyScriptHandler implements ScriptsHandler {

    public static final String FILENAME = "api.crt";

    private static final Logger log = LoggerFactory.getLogger(HostApiPublicKeyScriptHandler.class);

    @Inject
    HostApiService hostApiService;

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        String id = request.getId();

        if (!FILENAME.equals(id)) {
            return false;
        }

        String pem = null;

        for (Map.Entry<String, PublicKey> entry : hostApiService.getPublicKeys().entrySet()) {
            try {
                pem = SshKeyGen.writePublicKey(entry.getValue());
                break;
            } catch (Exception e) {
                log.error("Failed to write PEM", e);
            }
        }

        if (pem == null) {
            return false;
        }

        byte[] content = pem.getBytes("UTF-8");

        HttpServletResponse response = request.getServletContext().getResponse();

        response.getOutputStream().write(pem.getBytes("UTF-8"));
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
