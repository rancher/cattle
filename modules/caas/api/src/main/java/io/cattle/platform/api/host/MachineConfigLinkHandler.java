package io.cattle.platform.api.host;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.cattle.platform.api.host.HostsOutputFilter.*;
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
            String extractedConfig = (String) DataAccessor.getFields(host).get(EXTRACTED_CONFIG_FIELD);
            if (extractedConfig.startsWith("{")) {
                try {
                    extractedConfig = secretsService.decrypt(extractedConfig);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            if (StringUtils.isNotEmpty(extractedConfig)) {
                String filename = host.getName();
                if (StringUtils.isBlank(filename)) {
                    filename = DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME);
                }

                byte[] content = writeZip(host, extractedConfig);
                HttpServletResponse response = request.getServletContext().getResponse();
                response.setContentLength(content.length);
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".zip");
                response.setHeader("Cache-Control", "private");
                response.setHeader("Pragma", "private");
                response.setHeader("Expires", "Wed 24 Feb 1982 18:42:00 GMT");
                request.getOutputStream().write(content);
                return new Object();
            }
        }

        return null;
    }

    protected byte[] writeZip(Host host, String extractedConfig) throws IOException {
        if (extractedConfig.startsWith("{")) {
            try {
                extractedConfig = secretsService.decrypt(extractedConfig);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zof = new ZipOutputStream(baos);
        TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.decodeBase64(extractedConfig))));

        TarArchiveEntry entry;
        while ((entry = tais.getNextTarEntry()) != null) {
            if (entry.getSize() < 100) {
                continue;
            }
            String[] parts = entry.getName().split("/");
            if (parts.length != 4) {
                continue;
            }
            if (parts[3].equals("config.json")) {
                continue;
            }
            ZipEntry ze = new ZipEntry(String.format("%s/%s", parts[2], parts[3]));
            zof.putNextEntry(ze);
            IOUtils.copy(tais, zof);
            zof.closeEntry();
        }

        zof.close();
        return baos.toByteArray();
    }
}
