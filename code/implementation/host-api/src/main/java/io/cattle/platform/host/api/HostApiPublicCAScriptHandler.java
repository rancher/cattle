package io.cattle.platform.host.api;

import io.cattle.platform.core.dao.CertificateDao;
import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

public class HostApiPublicCAScriptHandler implements ScriptsHandler {

    public static final String FILENAME = "ca.crt";

    @Inject
    CertificateDao certDao;

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        String id = request.getId();

        if (!FILENAME.equals(id)) {
            return false;
        }

        String cert = certDao.getPublicCA();
        if (cert == null) {
            return false;
        }

        byte[] content = cert.getBytes("UTF-8");

        HttpServletResponse response = request.getServletContext().getResponse();

        response.getOutputStream().write(content);
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