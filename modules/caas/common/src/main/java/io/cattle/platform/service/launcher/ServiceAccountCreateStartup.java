package io.cattle.platform.service.launcher;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.util.type.InitializationTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class ServiceAccountCreateStartup extends GenericServiceLauncher implements InitializationTask {

    public ServiceAccountCreateStartup(LockManager lockManager, LockDelegator lockDelegator, ScheduledExecutorService executor, AccountDao accountDao,
            GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, ObjectProcessManager processManager) {
        super(lockManager, lockDelegator, executor, accountDao, resourceDao, resourceMonitor, processManager);
    }

    @Override
    public void start() {
        getCredential();
    }

    @Override
    protected boolean shouldRun() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String binaryPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

}