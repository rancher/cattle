package io.cattle.host.api.proxy.launch;

import static io.cattle.platform.server.context.ServerContext.*;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyLauncher extends NoExceptionRunnable implements InitializationTask, Runnable {

    private static final int WAIT = 2000;
    private static final Logger log = LoggerFactory.getLogger(ProxyLauncher.class);

    @Inject
    HostApiService hostApiService;

    @Inject
    ScheduledExecutorService executor;

    @Inject
    ServiceAccountCreateStartup serviceAccount;

    Process process;

    ScheduledFuture<?> future;

    @Override
    public void start() {
        if (StringUtils.equals(HOST_API_PROXY_MODE_EMBEDDED, getHostApiProxyMode())) {
            future = executor.scheduleWithFixedDelay(this, WAIT, WAIT, TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    processDestroy();
                }
            });
        }
    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(true);
        }

        processDestroy();
    }

    protected synchronized void processDestroy() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    @Override
    protected void doRun() throws Exception {
        if (!StringUtils.equals(HOST_API_PROXY_MODE_EMBEDDED, getHostApiProxyMode())) {
            return;
        }

        boolean launch = false;
        if (process == null) {
            launch = true;
        } else {
            try {
                process.exitValue();
                launch = true;
                process.waitFor();
            } catch (IllegalThreadStateException e) {
                // ignore
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (!launch) {
            return;
        }

        ProcessBuilder pb = new ProcessBuilder("websocket-proxy");

        Map<String, String> env = pb.environment();
        String pubKey = getPublicKey();
        if (pubKey == null) {
            throw new RuntimeException("Couldn't get public key for websocket-proxy.");
        }
        env.put("PATH", System.getenv("PATH"));
        env.put("PROXY_JWT_PUBLIC_KEY_CONTENTS", pubKey);
        env.put("PROXY_LISTEN_ADDRESS", ":" + getProxyPort());

        String cattleProxyAddress = "localhost:" + getProxiedPort();
        env.put("PROXY_CATTLE_ADDRESS", cattleProxyAddress);

        env.put("PROXY_HTTPS_PROXY_PROTOCOL_PORTS", getProxyProtocolHttpsPorts());

        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null) {
            String[] parts = processName.split("@");
            if (parts.length > 0 && StringUtils.isNotEmpty(parts[0])) {
                env.put("PROXY_PARENT_PID", parts[0]);
            }
        }

        Credential cred = serviceAccount.getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());

        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public String getPublicKey() {
        for (Map.Entry<String, PublicKey> entry : hostApiService.getPublicKeys().entrySet()) {
            try {
                return SshKeyGen.writePublicKey(entry.getValue());
            } catch (Exception e) {
                log.error("Failed to write PEM", e);
            }
        }
        return null;
    }
}
