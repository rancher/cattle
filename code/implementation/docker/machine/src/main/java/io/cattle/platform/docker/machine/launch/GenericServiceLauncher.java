package io.cattle.platform.docker.machine.launch;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

public abstract class GenericServiceLauncher extends NoExceptionRunnable implements InitializationTask, Runnable {

    private static final String SERVICE_USER_UUID = "machineServiceAccount";
    private static final int WAIT = 2000;

    @Inject
    LockDelegator lockDelegator;
    @Inject
    ScheduledExecutorService executor;
    @Inject
    AccountDao accountDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ResourceMonitor resourceMonitor;

    Process process;
    ScheduledFuture<?> future;

    @Override
    public void start() {
        future = executor.scheduleWithFixedDelay(this, WAIT, WAIT, TimeUnit.MILLISECONDS);
        ServerContext.HOST.addCallback(new Runnable() {
            @Override
            public void run() {
                processDestroy();
            }
        });
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

    protected String getAccountUuid() {
        return SERVICE_USER_UUID;
    }

    protected Credential getCredential() {
        Account account = accountDao.findByUuid(SERVICE_USER_UUID);
        if (account == null) {
            account = resourceDao.createAndSchedule(Account.class, ACCOUNT.UUID, getAccountUuid(), ACCOUNT.NAME, getAccountUuid(), ACCOUNT.KIND,
                    AccountConstants.SERVICE_KIND);
        }

        account = resourceMonitor.waitForState(account, CommonStatesConstants.ACTIVE);
        Credential cred = accountDao.getApiKey(account, false);

        if (cred == null) {
            cred = resourceDao.createAndSchedule(Credential.class, CREDENTIAL.KIND, CredentialConstants.KIND_API_KEY, CREDENTIAL.ACCOUNT_ID, account.getId());
        }

        return resourceMonitor.waitForState(cred, CommonStatesConstants.ACTIVE);
    }

    protected abstract boolean shouldRun();
    protected abstract String binaryPath();
    protected abstract void setEnvironment(Map<String, String> env);
    protected abstract LockDefinition getLock();
    
    @Override
    protected synchronized void doRun() throws Exception {
        if (!shouldRun() || !ServerContext.isCustomApiHost()) {
            return;
        }

        if (!lockDelegator.tryLock(getLock())) {
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

        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}