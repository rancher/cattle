package io.cattle.platform.agent.connection.ssh;

import io.cattle.platform.agent.connection.ssh.dao.SshAgentDao;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.LockCallbackWithException;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.util.type.InitializationTask;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class ClientKeyPairProvider extends AbstractKeyPairProvider implements InitializationTask {

    private static final DynamicBooleanProperty GEN_ON_STARTUP = ArchaiusUtil.getBoolean("ssh.keygen.on.startup");
    private static final Logger log = LoggerFactory.getLogger(ClientKeyPairProvider.class);

    volatile KeyPair[] keys = null;
    SshAgentDao agentDao;
    LockManager lockManager;
    ExecutorService executorService;

    @Override
    protected KeyPair[] loadKeys() {
        if (keys != null) {
            return keys;
        }

        try {
            readKeys();
            return keys;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get SSH key", e);
        }
    }

    protected synchronized void readKeys() throws Exception {
        List<String[]> keyStrings = agentDao.getClientKeyPairs();
        if (keyStrings.size() == 0) {
            keyStrings = generateKeys();
        }

        keys = read(keyStrings);
    }

    protected KeyPair[] read(List<String[]> keyStrings) throws Exception {
        List<KeyPair> keyPairs = new ArrayList<KeyPair>();

        for (String[] keyString : keyStrings) {
            KeyPair pair = SshKeyGen.readKeyPair(keyString[1]);
            keyPairs.add(pair);
        }

        return keyPairs.toArray(new KeyPair[keyPairs.size()]);
    }

    protected List<String[]> generateKeys() throws Exception {
        return lockManager.lock(new KeyGenLock(), new LockCallbackWithException<List<String[]>, Exception>() {
            @Override
            public List<String[]> doWithLock() throws Exception {
                return generateKeysWithLock();
            }
        }, Exception.class);
    }

    protected List<String[]> generateKeysWithLock() throws Exception {
        List<String[]> keyStrings = agentDao.getClientKeyPairs();
        if (keyStrings.size() > 0) {
            return keyStrings;
        }

        log.info("Generating SSH key");
        String[] kp = SshKeyGen.generateKeys();

        String publicString = kp[0];
        log.info("Generated [{}]", publicString);

        String pemString = kp[1];
        agentDao.saveKey(publicString, pemString);

        List<String[]> result = new ArrayList<String[]>();
        result.add(new String[] { publicString, pemString });
        return result;
    }

    public SshAgentDao getAgentDao() {
        return agentDao;
    }

    @Inject
    public void setAgentDao(SshAgentDao agentDao) {
        this.agentDao = agentDao;
    }

    @Override
    public void start() {
        if (GEN_ON_STARTUP.get()) {
            executorService.execute(new NoExceptionRunnable() {
                @Override
                protected void doRun() throws Exception {
                    SecurityUtils.getSecurityProvider();
                    loadKeys();
                }
            });
        }
    }

    @Override
    public void stop() {
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

}
