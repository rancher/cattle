package io.github.ibuildthecloud.agent.connection.ssh;

import io.github.ibuildthecloud.agent.connection.ssh.dao.SshAgentDao;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.lock.LockCallbackWithException;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class ClientKeyPairProvider extends AbstractKeyPairProvider implements InitializationTask {

    private static final DynamicBooleanProperty GEN_ON_STARTUP = ArchaiusUtil.getBoolean("ssh.keygen.on.startup");
    private static final Logger log = LoggerFactory.getLogger(ClientKeyPairProvider.class);
    private static final byte[] HEADER = new byte[] {'s', 's', 'h', '-', 'r', 's', 'a'};
    private static final String SSH_RSA_FORMAT = "ssh-rsa %s dstack@dstack";

    volatile KeyPair[] keys = null;
    SshAgentDao agentDao;
    LockManager lockManager;
    ExecutorService executorService;

    @Override
    protected KeyPair[] loadKeys() {
       if ( keys != null ) {
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
        if ( keyStrings.size() == 0 ) {
            keyStrings = generateKeys();
        }

        keys = read(keyStrings);
    }

    protected KeyPair[] read(List<String[]> keyStrings) throws Exception {
        List<KeyPair> keyPairs = new ArrayList<KeyPair>();

        for ( String[] keyString : keyStrings ) {
            KeyPair pair = readKeyPair(keyString[1]);
            keyPairs.add(pair);
        }

        return keyPairs.toArray(new KeyPair[keyPairs.size()]);
    }

    protected KeyPair readKeyPair(String key) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(key));
        PEMReader r = null;
        try {
            r = new PEMReader(new InputStreamReader(bais));
            return (KeyPair)r.readObject();
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    protected String writeKeyPair(KeyPair kp) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PEMWriter w = new PEMWriter(new OutputStreamWriter(baos));
        w.writeObject(kp);
        w.flush();
        IOUtils.closeQuietly(w);

        return Base64.encodeBase64String(baos.toByteArray());
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
        if ( keyStrings.size() > 0 ) {
            return keyStrings;
        }

        log.info("Generating SSH key");
        KeyPairGenerator generator = SecurityUtils.getKeyPairGenerator("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        String publicString = sshRsaTextFormat((RSAPublicKey)pair.getPublic());
        log.info("Generated [{}]", publicString);

        String pemString = writeKeyPair(pair);
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
        if ( GEN_ON_STARTUP.get() ) {
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


    public String sshRsaTextFormat(RSAPublicKey key) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, HEADER);
        write(out, key.getPublicExponent().toByteArray());
        write(out, key.getModulus().toByteArray());

        return String.format(SSH_RSA_FORMAT, Base64.encodeBase64String(out.toByteArray()));
    }

    protected void write(OutputStream os, byte[] content) throws IOException {
        byte[] length = new byte[4];
        length[0] = (byte)((content.length >>> 24) & 0xff);
        length[1] = (byte)((content.length >>> 16) & 0xff);
        length[2] = (byte)((content.length >>> 8) & 0xff);
        length[3] = (byte)(content.length & 0xff);

        os.write(length);
        os.write(content);
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
