package io.cattle.platform.launcher;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class TelemetryLauncher extends GenericServiceLauncher {

    private static final DynamicStringProperty TELEMETRY_BINARY = ArchaiusUtil.getString("telemetry.service.executable");
    private static final DynamicStringProperty LAUNCH_TELEMETRY = ArchaiusUtil.getString("telemetry.opt");

    ClusterService clusterService;

    public TelemetryLauncher(LockManager lockManager, LockDelegator lockDelegator, ScheduledExecutorService executor, AccountDao accountDao,
            GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, ObjectProcessManager processManager, ClusterService clusterService) {
        super(lockManager, lockDelegator, executor, accountDao, resourceDao, resourceMonitor, processManager);
        this.clusterService = clusterService;
    }

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
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        pb.command().add("client");
    }

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        return Arrays.asList(LAUNCH_TELEMETRY);
    }

}