package io.cattle.platform.host.service;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.cattle.platform.token.impl.RSAPrivateKeyHolder;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.type.InitializationTask;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class HostApiRSAKeyProvider implements RSAKeyProvider, InitializationTask {

    private static final DynamicBooleanProperty GEN_ON_STARTUP = ArchaiusUtil.getBoolean("host.api.keygen.on.startup");

    private static final String KEY = "host.api.key";
    private static final String CERT = "host.api.key.cert";
    private static final String DEFAULT = "default";

    DataDao dataDao;

    @Override
    public RSAPrivateKeyHolder getPrivateKey() {
        KeyPair kp = getKeyPair();
        if (kp == null) {
            return null;
        }
        return new RSAPrivateKeyHolder(DEFAULT, (RSAPrivateKey) kp.getPrivate());
    }

    @Override
    public void start() {
        if (GEN_ON_STARTUP.get()) {
            getPrivateKey();
            getCACertificate();
        }
    }

    @Override
    public void stop() {
    }

    protected KeyPair getKeyPair() {
        String encoded = dataDao.getOrCreate(KEY, false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                KeyPair kp = SshKeyGen.generateKeyPair();
                return SshKeyGen.toPEM(kp);
            }
        });

        try {
            return SshKeyGen.readKeyPair(encoded);
        } catch (Exception e) {
            ExceptionUtils.throwRuntime("Failed to read key pair from PEM", e);
            /* Won't hit next line */
            return null;
        }
    }

    protected X509Certificate getCACertificate() {
        final KeyPair kp = getKeyPair();
        String encoded = dataDao.getOrCreate(CERT, false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                X509Certificate cert = SshKeyGen.createRootCACert(kp);
                return SshKeyGen.toPEM(cert);
            }
        });

        try {
            return SshKeyGen.readCACert(encoded);
        } catch (Exception e) {
            ExceptionUtils.throwRuntime("Failed to CA cert from PEM", e);
            /* Won't hit next line */
            return null;
        }
    }

    @Override
    public CertSet generateCertificate(String subject) throws Exception {
        KeyPair caKp = getKeyPair();
        X509Certificate caCert = getCACertificate();
        KeyPair clientKp = SshKeyGen.generateKeyPair();
        X509Certificate clientCert = SshKeyGen.generateClientCert(subject, clientKp.getPublic(), caKp.getPrivate(), caCert);
        CertSet result = new CertSet(caCert, clientCert, clientKp.getPrivate());
        return result;
    }

    @Override
    public PublicKey getDefaultPublicKey() {
        return getPublicKeys().get(DEFAULT);
    }

    @Override
    public Map<String, PublicKey> getPublicKeys() {
        Map<String, PublicKey> result = new HashMap<String, PublicKey>();

        KeyPair defaultKp = getKeyPair();
        if (defaultKp != null) {
            result.put(DEFAULT, defaultKp.getPublic());
        }

        return result;
    }

    public DataDao getDataDao() {
        return dataDao;
    }

    @Inject
    public void setDataDao(DataDao dataDao) {
        this.dataDao = dataDao;
    }

}