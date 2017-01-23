package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.hazelcast.membership.ClusteredMember;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;


public class TraefikLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty BINARY = ArchaiusUtil.getString("traefik.executable");
    private static final DynamicStringProperty ACCESS_LOG = ArchaiusUtil.getString("access.log");
    public static final DynamicBooleanProperty LAUNCH = ArchaiusUtil.getBoolean("traefik.execute");
    public static final DynamicBooleanProperty PROXY = ArchaiusUtil.getBoolean("traefik.pass.proxy");

    private static final String[] PROXY_ENV = new String[] {
            "no_proxy",
            "http_proxy",
            "https_proxy",
            "NO_PROXY",
            "HTTP_PROXY",
            "HTTPS_PROXY"
    };
    private static final DynamicBooleanProperty API_FILTER_PROXY_LAUNCH = ArchaiusUtil.getBoolean("api.filter.proxy.execute");
    private static final DynamicStringProperty API_FILTER_PROXY_PORT = ArchaiusUtil.getString("api.filter.proxy.http.port");

    private static final Logger log = LoggerFactory.getLogger(TraefikLauncher.class);

    private static String CONFIG =
        "accessLogsFile = \"%s\"\n" +
        "[entryPoints]\n" +
        "  [entryPoints.http]\n" +
        "  address = \":8080\"\n" +
        "\n" +
        "[backends]\n" +
        "  [backends.wsp]\n" +
        "    [backends.wsp.servers.server1]\n" +
        "    url = \"http://%s\"\n" +
        "  [backends.cattle]\n" +
        "    [backends.cattle.servers.server1]\n" +
        "    url = \"http://localhost:%s\"\n" +
        "\n" +
        "[frontends]\n" +
        "  [frontends.wspfront]\n" +
        "  entryPoints = [\"http\"]\n" +
        "  passHostHeader = true\n" +
        "  backend = \"wsp\"\n" +
        "    [frontends.wspfront.routes.paths]\n" +
        "    rule = \"PathPrefix: /v1/connectbackend, /v2-beta/connectbackend, /v1/logs, /v1/stats, /v1/exec, /v1/console, /v1/dockersocket, "
        + "/v2-beta/logs, /v2-beta/stats, /v2-beta/exec, /v2-beta/console, /v2-beta/dockersocket, /v1/container-proxy, /v2-beta/container-proxy, "
        + "/r, /v1/hoststats, /v1/containerstats, /v2-beta/hoststats, /v2-beta/containerstats\"\n" +
        "  [frontends.cattlefront]\n" +
        "  entryPoints = [\"http\"]\n" +
        "  passHostHeader = true\n" +
        "  backend = \"cattle\"\n";
    String written = "";

    @Inject
    ClusterService clusterService;

    @Override
    protected boolean shouldRun() {
        return LAUNCH.get();
    }

    @Override
    protected boolean isReady() {
        String host = "localhost:8082";
        if (!clusterService.isMaster()) {
            ClusteredMember member = clusterService.getMaster();
            if (member != null) {
                host = String.format("%s:%d", member.getAdvertiseAddress(), member.getHttpPort());
            }
        }

        String port = "8081";
        if (API_FILTER_PROXY_LAUNCH.get()) {
            port = API_FILTER_PROXY_PORT.get();
        }

        String content = String.format(CONFIG, ACCESS_LOG.get(), host, port);
        if (written.equals(content)) {
            return true;
        }

        try (FileOutputStream fos = new FileOutputStream(new File("traefik.toml"))) {
            IOUtils.write(content, fos, "UTF-8");
            written = content;
        } catch (IOException e) {
            log.error("Failed to write configuration", e);
            return false;
        }

        return true;
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        super.prepareProcess(pb);

        pb.command(binaryPath(), "-c", "traefik.toml", "--file");
    }

    @Override
    protected String binaryPath() {
        return BINARY.get();
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        if (PROXY.get()) {
            return;
        }

        for (String i : PROXY_ENV) {
            env.remove(i);
        }
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

}
