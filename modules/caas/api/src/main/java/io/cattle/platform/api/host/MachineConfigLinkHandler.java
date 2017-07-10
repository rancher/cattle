package io.cattle.platform.api.host;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static io.cattle.platform.api.host.MachineLinkFilter.*;
import static io.cattle.platform.core.constants.HostConstants.*;

public class MachineConfigLinkHandler implements LinkHandler {

    ObjectManager objectManager;
    SecretsService secretsService;


    public MachineConfigLinkHandler(ObjectManager objectManager, SecretsService secretsService) {
        super();
        this.objectManager = objectManager;
        this.secretsService = secretsService;
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
            Host host = (Host) obj;
            String extractedConfig = (String) DataUtils.getFields(host).get(EXTRACTED_CONFIG_FIELD);
            if (extractedConfig.startsWith("{")) {
                try {
                    extractedConfig = secretsService.decrypt(host.getAccountId(), extractedConfig);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
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
