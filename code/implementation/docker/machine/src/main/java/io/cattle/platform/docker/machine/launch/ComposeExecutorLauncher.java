package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;


public class ComposeExecutorLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty COMPOSE_EXECUTOR_BINARY = ArchaiusUtil.getString("compose.executor.service.executable");
    private static final DynamicStringProperty COMPOSE_EXECUTOR_CLIENT_TIMEOUT = ArchaiusUtil.getString("compose.executor.service.executable.timeout");
    private static final DynamicBooleanProperty LAUNCH_COMPOSE_EXECUTOR = ArchaiusUtil.getBoolean("compose.executor.execute");

    @Inject
    ClusterService clusterService;

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
        env.put("RANCHER_CLIENT_TIMEOUT", COMPOSE_EXECUTOR_CLIENT_TIMEOUT.get());
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
    protected List<DynamicStringProperty> getReloadSettings() {
        List<DynamicStringProperty> list = new ArrayList<>();
        list.add(COMPOSE_EXECUTOR_CLIENT_TIMEOUT);
        return list;
    }

}
