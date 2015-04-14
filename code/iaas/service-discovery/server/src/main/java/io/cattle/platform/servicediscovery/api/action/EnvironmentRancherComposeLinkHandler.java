package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

public class EnvironmentRancherComposeLinkHandler implements LinkHandler {
    @Inject
    ServiceDiscoveryService discoverySvc;

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { "environment" };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return ServiceDiscoveryConstants.LINK_RANCHER_COMPOSE_CONFIG.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Environment env = (Environment) obj;
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID, env.getId(),
                SERVICE.REMOVED, null);
        String rancherCompose = discoverySvc.buildRancherComposeConfig(services);
        byte[] content = rancherCompose.getBytes("UTF-8");
        HttpServletResponse response = request.getServletContext().getResponse();
        response.setContentLength(content.length);
        response.setHeader("Content-Disposition", "attachment; filename=rancher-compose.yml");
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "private");
        response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
        response.getOutputStream().write(content);
        return new Object();
    }
}
