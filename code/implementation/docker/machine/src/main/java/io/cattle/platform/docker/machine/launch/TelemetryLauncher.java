package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class TelemetryLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty TELEMETRY_BINARY = ArchaiusUtil.getString("telemetry.service.executable");
    private static final DynamicStringProperty LAUNCH_TELEMETRY = ArchaiusUtil.getString("telemetry.opt");

    @Inject
    ClusterService clusterService;

    @Override
    protected boolean shouldRun() {
        return "in".equals(LAUNCH_TELEMETRY.get()) && clusterService.isMaster();
    }

    @Override
    protected String binaryPath() {
        return TELEMETRY_BINARY.get();
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        Credential cred = getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
        env.put("CATTLE_URL", ServerContext.getLocalhostUrl(BaseProtocol.HTTP));
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        pb.command().add("client");
    }

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        return Arrays.asList(LAUNCH_TELEMETRY);
    }

}