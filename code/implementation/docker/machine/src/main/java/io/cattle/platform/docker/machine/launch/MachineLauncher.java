package io.cattle.platform.docker.machine.launch;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.config.ScopedConfig;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class MachineLauncher extends NoExceptionRunnable implements InitializationTask, Runnable {

    private static final DynamicStringProperty MACHINE_BINARY = ArchaiusUtil.getString("machine.service.executable");
    private static final DynamicBooleanProperty LAUNCH_MACHINE = ArchaiusUtil.getBoolean("machine.execute");

    private static final String MACHINE_USER_UUID = "machineServiceAccount";
    private static final int WAIT = 2000;

    @Inject LockDelegator lockDelegator;
    @Inject ScheduledExecutorService executor;
    @Inject AccountDao accountDao;
    @Inject GenericResourceDao resourceDao;
    @Inject ResourceMonitor resourceMonitor;
    @Inject ScopedConfig scopedConfig;

    Process process;
    ScheduledFuture<?> future;

    @Override
    public void start() {
        future = executor.scheduleWithFixedDelay(this, WAIT, WAIT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if ( future != null ) {
            future.cancel(true);
        }
        if ( process != null ) {
            process.destroy();
        }
    }

    protected String getAccountUuid() {
        return MACHINE_USER_UUID;
    }

    protected Credential getCredential() {
        Account account = accountDao.findByUuid(MACHINE_USER_UUID);
        if ( account == null ) {
            account = resourceDao.createAndSchedule(Account.class,
                    ACCOUNT.UUID, getAccountUuid(),
                    ACCOUNT.NAME, getAccountUuid(),
                    ACCOUNT.KIND, AccountConstants.SERVICE_KIND);
        }

        account = resourceMonitor.waitForState(account, CommonStatesConstants.ACTIVE);
        Credential cred = accountDao.getApiKey(account, false);

        if ( cred == null ) {
            cred = resourceDao.createAndSchedule(Credential.class,
                    CREDENTIAL.KIND, CredentialConstants.KIND_API_KEY,
                    CREDENTIAL.ACCOUNT_ID, account.getId());
        }

        return resourceMonitor.waitForState(cred, CommonStatesConstants.ACTIVE);
    }

    @Override
    protected void doRun() throws Exception {
        if ( ! LAUNCH_MACHINE.get() ) {
            return;
        }

        if ( ! lockDelegator.tryLock(new MachineLauncherLock()) ) {
            return;
        }

        boolean launch = false;
        if ( process == null ) {
            launch = true;
        } else {
            try {
                process.exitValue();
                launch = true;
                process.waitFor();
            } catch (IllegalThreadStateException e) {
                //ignore
            } catch (InterruptedException e) {
                //ignore
            }
        }

        if ( ! launch ) {
            return;
        }

        Credential cred = getCredential();
        ProcessBuilder pb = new ProcessBuilder(MACHINE_BINARY.get());
        Map<String,String> env = pb.environment();

        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
        env.put("CATTLE_URL", scopedConfig.getApiUrl(null));

        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
