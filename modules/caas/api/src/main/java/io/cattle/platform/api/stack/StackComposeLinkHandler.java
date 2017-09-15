package io.cattle.platform.api.stack;

import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.TypeIdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        IdFormatter idFormatter = new TypeIdFormatter(request.getSchemaFactory());
        String compose = composeExportService.buildComposeConfig(idFormatter.formatId(stack.getKind(), stack.getId()).toString());

        if (StringUtils.isNotEmpty(compose)) {
            ByteArrayOutputStream baos = zipFiles(compose);
            HttpServletResponse response = request.getServletContext().getResponse();
            response.setContentLength(baos.toByteArray().length);
            response.setContentType("application/zip");
            response.setHeader("Content-Encoding", "zip");
            response.setHeader("Content-Disposition", String.format("attachment; filename=%s.zip", stack.getName()));
            response.setHeader("Cache-Control", "private");
            response.setHeader("Pragma", "private");
            response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
            response.getOutputStream().write(baos.toByteArray());
            return new Object();
        }
        return null;
    }

    private ByteArrayOutputStream zipFiles(String compose) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zipFile(compose, zos, "compose.yml");
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
