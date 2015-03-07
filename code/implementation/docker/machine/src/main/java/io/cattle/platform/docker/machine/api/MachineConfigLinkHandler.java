package io.cattle.platform.docker.machine.api;

import static io.cattle.platform.docker.machine.constants.MachineConstants.*;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

public class MachineConfigLinkHandler implements LinkHandler {

    @Override
    public String[] getTypes() {
        return new String[] { MACHINE_KIND };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return CONFIG_LINK.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (obj instanceof PhysicalHost) {
            PhysicalHost host = (PhysicalHost) obj;
            String extractedConfig = (String) DataUtils.getFields(host).get(EXTRACTED_CONFIG_FIELD);
            if (StringUtils.isNotEmpty(extractedConfig)) {
                byte[] content = Base64.decodeBase64(extractedConfig.getBytes());
                HttpServletResponse response = request.getServletContext().getResponse();
                response.setContentLength(content.length);
                response.setContentType("application/x-tar");
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Disposition", "attachment; filename=" + host.getName() + ".tar.gz");
                response.setHeader("Cache-Control", "private");
                response.setHeader("Pragma", "private");
                response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
                response.getOutputStream().write(content);
                return new Object();
            }
        }

        return null;
    }
}
