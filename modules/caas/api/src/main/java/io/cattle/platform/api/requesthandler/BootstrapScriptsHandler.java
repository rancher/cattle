package io.cattle.platform.api.requesthandler;

import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.util.StandardCharset;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;

public class BootstrapScriptsHandler implements ScriptsHandler {

    private static final DynamicStringProperty BOOTSTRAP_SOURCE = ArchaiusUtil.getString("bootstrap.source");
    private static final DynamicStringProperty REQUIRED_IMAGE = ArchaiusUtil.getString("bootstrap.required.image");

    private static final String BOOTSTRAP = "bootstrap";

    RSAKeyProvider keyProvider;

    public BootstrapScriptsHandler(RSAKeyProvider keyProvider) {
        super();
        this.keyProvider = keyProvider;
    }

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        if (!BOOTSTRAP.equals(request.getId())) {
            return false;
        }

        byte[] content = getBootstrapSource(request);
        IOUtils.copy(new ByteArrayInputStream(content), request.getServletContext().getResponse().getOutputStream());

        return true;
    }

    protected byte[] getBootstrapSource(ApiRequest apiRequest) throws IOException {
        ClassLoader cl = BootstrapScriptsHandler.class.getClassLoader();
        Certificate cert = keyProvider.getCACertificate();
        byte[] pem = keyProvider.toBytes(cert);
        try (InputStream is = cl.getResourceAsStream(BOOTSTRAP_SOURCE.get())) {
            String content = IOUtils.toString(is, StandardCharset.UTF_8);
            content = content.replace("REQUIRED_IMAGE=", String.format("REQUIRED_IMAGE=\"%s\"", REQUIRED_IMAGE.get()));
            content = content.replace("DETECTED_CATTLE_AGENT_IP=", String.format("DETECTED_CATTLE_AGENT_IP=\"%s\"", apiRequest.getClientIp()));
            content = content.replace("%CERT%", new String(pem, "UTF-8"));
            return content.getBytes("UTF-8");
        }
    }

}
