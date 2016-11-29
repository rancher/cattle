package io.cattle.platform.bootstrap.script;

import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class BootstrapScriptsHandler implements ScriptsHandler {

    private static final String BOOTSTRAP = "bootstrap";

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        if (!BOOTSTRAP.equals(request.getId())) {
            return false;
        }

        byte[] content = BootstrapScript.getBootStrapSource(request);
        IOUtils.copy(new ByteArrayInputStream(content), request.getServletContext().getResponse().getOutputStream());

        return true;
    }

}
