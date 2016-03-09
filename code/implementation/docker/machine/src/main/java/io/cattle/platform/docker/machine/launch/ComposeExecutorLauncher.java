package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.service.launcher.LauncherLockDefinitions;
import io.cattle.platform.util.type.InitializationTask;

import java.util.Map;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;


public class ComposeExecutorLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty COMPOSE_EXECUTOR_BINARY = ArchaiusUtil.getString("compose.executor.service.executable");
    private static final DynamicBooleanProperty LAUNCH_COMPOSE_EXECUTOR = ArchaiusUtil.getBoolean("compose.executor.execute");

    @Override
    protected boolean shouldRun() {
        return LAUNCH_COMPOSE_EXECUTOR.get();
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
        return LauncherLockDefinitions.ComposeExecutorLauncherLock();
    }

    @Override
    protected boolean isReady() {
        return true;
    }

}
