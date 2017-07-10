package io.cattle.platform.launcher;

import com.netflix.config.DynamicBooleanProperty;
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

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;


public class ComposeExecutorLauncher extends GenericServiceLauncher {

    private static final DynamicStringProperty COMPOSE_EXECUTOR_BINARY = ArchaiusUtil.getString("compose.executor.service.executable");
    private static final DynamicBooleanProperty LAUNCH_COMPOSE_EXECUTOR = ArchaiusUtil.getBoolean("compose.executor.execute");

    ClusterService clusterService;

    public ComposeExecutorLauncher(LockManager lockManager, LockDelegator lockDelegator, ScheduledExecutorService executor, AccountDao accountDao,
            GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, ObjectProcessManager processManager, ClusterService clusterService) {
        super(lockManager, lockDelegator, executor, accountDao, resourceDao, resourceMonitor, processManager);
        this.clusterService = clusterService;
    }

    @Override
    protected boolean shouldRun() {
        return LAUNCH_COMPOSE_EXECUTOR.get() && clusterService.isMaster();
    }

    @Override
    protected String binaryPath() {
        return COMPOSE_EXECUTOR_BINARY.get();
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

}
