package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.cattle.platform.token.impl.RSAPrivateKeyHolder;
import io.cattle.platform.util.type.InitializationTask;

import java.security.PublicKey;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class WebhookServiceLauncher extends GenericServiceLauncher implements InitializationTask {
    @Inject
    RSAKeyProvider keyProvider;

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceLauncher.class);

    private static final DynamicStringProperty WEBHOOK_SERVICE_BINARY = ArchaiusUtil.getString("webhook.service.executable");
    private static final DynamicBooleanProperty LAUNCH_WEBHOOK_SERVICE = ArchaiusUtil.getBoolean("webhook.service.execute");

    @Override
    protected boolean shouldRun() {
        return LAUNCH_WEBHOOK_SERVICE.get();
    }

    @Override
    protected String binaryPath() {
        return WEBHOOK_SERVICE_BINARY.get();
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        Credential cred = getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
        env.put("CATTLE_URL", ServerContext.getLocalhostUrl(BaseProtocol.HTTP));
        String publicKey = getPublicKey();
        if (publicKey == null) {
            throw new RuntimeException("Couldn't get public key for webhook-service.");
        }
        env.put("RSA_PUBLIC_KEY_CONTENTS", publicKey);
        String privateKey = getPrivateKey();
        if (privateKey == null) {
            throw new RuntimeException("Couldn't get private key for webhook-service.");
        }
        env.put("RSA_PRIVATE_KEY_CONTENTS", privateKey);

    }

    public String getPublicKey() {
        for (Map.Entry<String, PublicKey> entry : keyProvider.getPublicKeys().entrySet()) {
            try {
                return SshKeyGen.writePublicKey(entry.getValue());
            } catch (Exception e) {
                log.error("getPublicKey: Failed to write PEM", e);
            }
        }
        return null;
    }

    public String getPrivateKey() {
        RSAPrivateKeyHolder keyHolder = keyProvider.getPrivateKey();
        if(keyHolder == null) {
            return null;
        }
        try {
            return SshKeyGen.toPEM(keyProvider.getPrivateKey().getKey());
        } catch (Exception e) {
            log.error("getPrivateKey: Failed to write PEM", e);
            return null;
        }
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
