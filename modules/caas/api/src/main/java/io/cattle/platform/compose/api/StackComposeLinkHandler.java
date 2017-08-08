package io.cattle.platform.compose.api;

import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

public class StackComposeLinkHandler implements LinkHandler {

    ComposeExportService composeExportService;
    ObjectManager objectManager;

    public StackComposeLinkHandler(ComposeExportService composeExportService, ObjectManager objectManager) {
        super();
        this.composeExportService = composeExportService;
        this.objectManager = objectManager;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return ServiceConstants.LINK_COMPOSE_CONFIG.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Stack stack = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null);
        String dockerCompose = composeExportService.buildDockerComposeConfig(services, stack, false);
        String rancherCompose = composeExportService.buildRancherComposeConfig(services, false);
        String compose = composeExportService.buildDockerComposeConfig(services, stack, true);
        if (StringUtils.isNotEmpty(dockerCompose) || StringUtils.isNotEmpty(rancherCompose)) {
            ByteArrayOutputStream baos = zipFiles(dockerCompose, rancherCompose, compose);
            HttpServletResponse response = request.getServletContext().getResponse();
            response.setContentLength(baos.toByteArray().length);
            response.setContentType("application/zip");
            response.setHeader("Content-Encoding", "zip");
            response.setHeader("Content-Disposition", "attachment; filename=compose.zip");
            response.setHeader("Cache-Control", "private");
            response.setHeader("Pragma", "private");
            response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
            response.getOutputStream().write(baos.toByteArray());
            return new Object();
        }
        return null;
    }

    private ByteArrayOutputStream zipFiles(String dockerCompose, String rancherCompose, String compose) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            zipFile(dockerCompose, zos, "docker-compose.yml");
            zipFile(rancherCompose, zos, "rancher-compose.yml");
            zipFile(compose, zos, "compose.yml");
        } finally {
            zos.flush();
            baos.flush();
            zos.close();
            baos.close();
        }

        return baos;
    }

    private void zipFile(String compose, ZipOutputStream zos, String name) throws IOException {
        if (StringUtils.isNotEmpty(compose)) {
            byte[] content = compose.getBytes("UTF-8");
            zos.putNextEntry(new ZipEntry(name));
            zos.write(content);
            zos.closeEntry();
        }
    }
}
