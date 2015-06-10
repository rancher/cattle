package io.cattle.host.api.proxy.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.server.context.ServerContext;
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

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class ProxyLauncher extends NoExceptionRunnable implements InitializationTask, Runnable {

    private static final DynamicBooleanProperty HA_PROXY_MODE = ArchaiusUtil.getBoolean("host.api.proxy.hamode");
    private static final DynamicStringProperty PROXY_PORT = ArchaiusUtil.getString("host.api.proxy.port");

    private static final int WAIT = 2000;
    private static final Logger log = LoggerFactory.getLogger(ProxyLauncher.class);

    @Inject
    HostApiService hostApiService;

    @Inject
    ScheduledExecutorService executor;

    Process process;

    ScheduledFuture<?> future;

    @Override
    public void start() {
        boolean launch = !HA_PROXY_MODE.get();
        if (launch) {
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
        if(HA_PROXY_MODE.get()) {
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
        env.put("PROXY_LISTEN_ADDRESS", ":" + PROXY_PORT.get());
        
        String cattleProxyAddress = "localhost:" + ServerContext.HTTP_PORT.get();
        env.put("PROXY_CATTLE_ADDRESS", cattleProxyAddress);
        
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null) {
            String[] parts = processName.split("@");
            if (parts.length > 0 && StringUtils.isNotEmpty(parts[0])) {
                env.put("PROXY_PARENT_PID", parts[0]);
            }
        }

        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
