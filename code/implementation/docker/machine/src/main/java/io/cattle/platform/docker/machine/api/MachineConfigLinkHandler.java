package io.cattle.platform.docker.machine.api;

import static io.cattle.platform.core.constants.MachineConstants.*;
import static io.cattle.platform.docker.machine.api.MachineLinkFilter.*;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

public class MachineConfigLinkHandler implements LinkHandler {

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { KIND_MACHINE, HostConstants.TYPE };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return CONFIG_LINK.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (!canAccessConfig()){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        if (obj instanceof Host) {
            Long physicalHostId = ((Host) obj).getPhysicalHostId();
            obj = objectManager.loadResource(PhysicalHost.class, physicalHostId);
        }
        if (obj instanceof PhysicalHost) {
            PhysicalHost host = (PhysicalHost) obj;
            String extractedConfig = (String) DataUtils.getFields(host).get(EXTRACTED_CONFIG_FIELD);
            if (StringUtils.isNotEmpty(extractedConfig)) {
                byte[] content = Base64.decodeBase64(extractedConfig.getBytes());
                HttpServletResponse response = request.getServletContext().getResponse();
                response.setContentLength(content.length);
                response.setContentType("application/octet-stream");
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
