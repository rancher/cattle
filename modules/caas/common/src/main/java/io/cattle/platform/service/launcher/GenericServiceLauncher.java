package io.cattle.platform.service.launcher;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.util.ProcessUtils;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;

public abstract class GenericServiceLauncher extends NoExceptionRunnable implements Runnable {

    private static final String SERVICE_USER_UUID = "serviceAccount";
    private static final String SERVICE_USER_NAME = "System Service";
    private static final int WAIT = 2000;
    private final Logger log = LoggerFactory.getLogger(getClass());

    LockManager lockManager;
    LockDelegator lockDelegator;
    ScheduledExecutorService executor;
    AccountDao accountDao;
    GenericResourceDao resourceDao;
    ResourceMonitor resourceMonitor;
    ObjectProcessManager processManager;

    Process process;
    ScheduledFuture<?> future;

    public GenericServiceLauncher(LockManager lockManager, LockDelegator lockDelegator, ScheduledExecutorService executor, AccountDao accountDao,
            GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, ObjectProcessManager processManager) {
        super();
        this.lockManager = lockManager;
        this.lockDelegator = lockDelegator;
        this.executor = executor;
        this.accountDao = accountDao;
        this.resourceDao = resourceDao;
        this.resourceMonitor = resourceMonitor;
        this.processManager = processManager;
    }

    public void start() {
        Runnable cb = (new Runnable() {
            @Override
            public void run() {
                reload();
            }
        });
        future = executor.scheduleWithFixedDelay(this, WAIT, WAIT, TimeUnit.MILLISECONDS);
        List<DynamicStringProperty> reloadList = getReloadSettings();
        if (reloadList != null) {
            for(DynamicStringProperty reload : reloadList) {
                if (reload != null) {
                    reload.addCallback(cb);
                }
            }
        }
    }

    public void reload() {
        processDestroy();
    }

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

    protected String getAccountUuid() {
        return SERVICE_USER_UUID;
    }

    protected String getAccountName() {
        return SERVICE_USER_NAME;
    }

    protected List<DynamicStringProperty> getReloadSettings() {
        return null;
    }

    public Credential getCredential() {
        return lockManager.lock(new ServiceCredLock(SERVICE_USER_UUID), new LockCallback<Credential>() {
            @Override
            public Credential doWithLock() {
                return getCredentialLock();
            }
        });
    }

    protected Credential getCredentialLock() {
        Account account = accountDao.findByUuid(SERVICE_USER_UUID);
        if (account == null) {
            account = DeferredUtils.nest(new Callable<Account>() {
                @Override
                public Account call() throws Exception {
                    return resourceDao.createAndSchedule(Account.class, ACCOUNT.UUID, getAccountUuid(), ACCOUNT.NAME, getAccountName(), ACCOUNT.KIND,
                            AccountConstants.SERVICE_KIND);
                }
            });
        }

        final Long accountId = account.getId();
        // NOTE: It is find to block here because this is during startup or other non-important threads
        account = AsyncUtils.get(resourceMonitor.waitForState(account, CommonStatesConstants.ACTIVE));
        List<? extends Credential> creds = accountDao.getApiKeys(account, CredentialConstants.KIND_AGENT_API_KEY, false);
        Credential cred = creds.size() > 0 ? creds.get(0) : null;

        /* This is to fix a bug in which we ended up with a lot of api keys created */
        for (int i = 1; i < creds.size(); i++) {
            processManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, creds.get(i), ProcessUtils.chainInData(new HashMap<>(),
                    CredentialConstants.PROCESSS_DEACTIVATE, CredentialConstants.PROCESSS_REMOVE));
        }

        if (cred == null) {
            cred = DeferredUtils.nest(new Callable<Credential>() {
                @Override
                public Credential call() throws Exception {
                    return resourceDao.createAndSchedule(Credential.class, CREDENTIAL.KIND, CredentialConstants.KIND_AGENT_API_KEY, CREDENTIAL.ACCOUNT_ID,
                            accountId);
                }
            });
        }

        return AsyncUtils.get(resourceMonitor.waitForState(cred, CommonStatesConstants.ACTIVE));
    }

    protected abstract boolean shouldRun();
    protected abstract boolean isReady();
    protected abstract String binaryPath();
    protected abstract void setEnvironment(Map<String, String> env);
    protected abstract LockDefinition getLock();

    @Override
    protected synchronized void doRun() throws Exception {
        if (!ProcessEngineUtils.enabled() || !shouldRun() || !isReady()) {
            return;
        }

        LockDefinition lock = getLock();

        if (lock != null && !lockDelegator.tryLock(lock)) {
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

        ProcessBuilder pb = new ProcessBuilder(binaryPath());
        Map<String, String> env = pb.environment();

        setEnvironment(env);
        prepareProcess(pb);

        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        try {
            process = pb.start();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    protected void prepareProcess(ProcessBuilder pb) throws IOException {
    }
}