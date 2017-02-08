package io.cattle.platform.docker.machine.launch;

import static io.cattle.platform.server.context.ServerContext.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.hazelcast.membership.ClusteredMember;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class WebsocketProxyLauncher extends GenericServiceLauncher {

    private static final Logger log = LoggerFactory.getLogger(WebsocketProxyLauncher.class);
    private static final String MASTER_CONF = "master.conf";

    private static final DynamicStringProperty ACCESS_LOG = ArchaiusUtil.getString("access.log");
    private static final DynamicStringProperty API_INTERCEPTOR_CONFIG = ArchaiusUtil.getString("api.interceptor.config");
    private static final DynamicStringProperty API_INTERCEPTOR_CONFIG_FILE = ArchaiusUtil.getString("api.interceptor.config.file");

    @Inject
    ClusterService clusterService;

    String written = "start";

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        List<DynamicStringProperty> list = new ArrayList<DynamicStringProperty>();
        list.add(ACCESS_LOG);
        list.add(API_INTERCEPTOR_CONFIG);
        return list;
    }

    protected void prepareConfigFile() throws IOException {
        String config = API_INTERCEPTOR_CONFIG.get();
        if (StringUtils.isBlank(config)) {
            new File(API_INTERCEPTOR_CONFIG_FILE.get()).delete();
        } else {
            try(FileWriter fw = new FileWriter(API_INTERCEPTOR_CONFIG_FILE.get())) {
                IOUtils.write(API_INTERCEPTOR_CONFIG.get(), fw);
            }
        }
    }

    @Override
    protected boolean shouldRun() {
        return StringUtils.equals(HOST_API_PROXY_MODE_EMBEDDED, getHostApiProxyMode());
    }

    @Override
    protected boolean isReady() {
        String host = "";
        if (!clusterService.isMaster()) {
            ClusteredMember member = clusterService.getMaster();
            if (member != null) {
                host = String.format("%s:%d", member.getAdvertiseAddress(), member.getHttpPort());
            }
        }

        if (written.equals(host)) {
            return true;
        }

        try (FileWriter fw = new FileWriter(new File(MASTER_CONF + ".tmp"))) {
            IOUtils.write(host, fw);
            written = host;
        } catch (IOException e) {
            log.error("Failed to write configuration", e);
            return false;
        }

        return new File(MASTER_CONF + ".tmp").renameTo(new File(MASTER_CONF));
    }


    @Override
    protected String binaryPath() {
        return "websocket-proxy";
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        env.clear();
        String cattleProxyAddress = "localhost:" + getProxiedPort();

        env.put("PATH", System.getenv("PATH"));
        env.put("PROXY_LISTEN_ADDRESS", ":" + getProxyPort());
        env.put("PROXY_TLS_LISTEN_ADDRESS", ":" + getProxyPort());
        env.put("PROXY_MASTER_FILE", MASTER_CONF);
        env.put("PROXY_CATTLE_ADDRESS", cattleProxyAddress);
        env.put("PROXY_HTTPS_PROXY_PROTOCOL_PORTS", getProxyProtocolHttpsPorts());
        env.put("PROXY_API_INTERCEPTOR_CONFIG_FILE", API_INTERCEPTOR_CONFIG_FILE.get());

        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null) {
            String[] parts = processName.split("@");
            if (parts.length > 0 && StringUtils.isNotEmpty(parts[0])) {
                env.put("PROXY_PARENT_PID", parts[0]);
            }
        }

        Credential cred = getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        super.prepareProcess(pb);
        prepareConfigFile();
    }

    @Override
    public void reload() {
        if (!shouldRun()) {
            return;
        }

        try {
            prepareConfigFile();
            StringBuilder apiProxyUrl = new StringBuilder();
            apiProxyUrl.append("http://localhost:").append(getProxyPort()).append("/v1-api-interceptor/reload");
            Request.Post(apiProxyUrl.toString()).execute();
        } catch (IOException e) {
            log.error("Failed to reload api proxy service: {}", e.getMessage());
        }
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    private String getProxiedPort() {
        // To match the functionality in the Jetty Main class, need to get value this way as opposed
        // to using ArchaiusUtils
        String port = System.getenv("CATTLE_HTTP_PROXIED_PORT");
        return port == null ? System.getProperty("cattle.http.proxied.port", "8081") : port;
    }

    private String getProxyPort() {
        // To match the functionality in the Jetty Main class, need to get value this way as opposed
        // to using ArchaiusUtils
        String port = System.getenv("CATTLE_HTTP_PORT");
        return port == null ? System.getProperty("cattle.http.port", "8080") : port;
    }

    private String getProxyProtocolHttpsPorts() {
        String ports = System.getenv("PROXY_PROTOCOL_HTTPS_PORTS");
        return ports == null ? System.getProperty("proxy.protocol.https.ports", "443") : ports;
    }

}
