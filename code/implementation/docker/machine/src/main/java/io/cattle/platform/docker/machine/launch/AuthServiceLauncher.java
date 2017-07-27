package io.cattle.platform.docker.machine.launch;

import static io.cattle.platform.core.model.tables.SettingTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.external.ServiceAuthConstants;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.cattle.platform.token.impl.RSAPrivateKeyHolder;
import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;


public class AuthServiceLauncher extends GenericServiceLauncher implements InitializationTask {
    @Inject
    RSAKeyProvider keyProvider;

    @Inject
    ObjectManager objectManager;

    private static final Logger log = LoggerFactory.getLogger(AuthServiceLauncher.class);

    private static final DynamicStringProperty AUTH_SERVICE_BINARY = ArchaiusUtil.getString("auth.service.executable");
    private static final DynamicBooleanProperty LAUNCH_AUTH_SERVICE = ArchaiusUtil.getBoolean("auth.service.execute");

    public static final DynamicStringProperty SECURITY_SETTING = ArchaiusUtil.getString("api.security.enabled");
    public static final DynamicStringProperty EXTERNAL_AUTH_PROVIDER_SETTING = ArchaiusUtil.getString("api.auth.external.provider.configured");
    public static final DynamicStringProperty NO_IDENTITY_LOOKUP_SETTING = ArchaiusUtil.getString("api.auth.external.provider.no.identity.lookup");

    @Override
    protected boolean shouldRun() {
        return LAUNCH_AUTH_SERVICE.get();
    }

    @Override
    protected String binaryPath() {
        return AUTH_SERVICE_BINARY.get();
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        Credential cred = getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
        env.put("CATTLE_URL", ServerContext.getLocalhostUrl(BaseProtocol.HTTP));
        String pubKey = getPublicKey();
        if (pubKey == null) {
            throw new RuntimeException("Couldn't get public key for auth-service.");
        }
        env.put("RSA_PUBLIC_KEY_CONTENTS", pubKey);
        String privateKey = getPrivateKey();
        if (privateKey == null) {
            throw new RuntimeException("Couldn't get private key for auth-service.");
        }
        env.put("RSA_PRIVATE_KEY_CONTENTS", privateKey);
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    protected boolean isReady() {
        return true;
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
    protected List<DynamicStringProperty> getReloadSettings() {
        List<DynamicStringProperty> list = new ArrayList<DynamicStringProperty>();
        list.add(SecurityConstants.AUTH_PROVIDER);
        list.add(SecurityConstants.AUTH_ENABLER_SETTING);
        list.add(SECURITY_SETTING);
        list.add(ServiceAuthConstants.ACCESS_MODE);
        list.add(ServiceAuthConstants.ALLOWED_IDENTITIES);
        list.add(EXTERNAL_AUTH_PROVIDER_SETTING);
        list.add(NO_IDENTITY_LOOKUP_SETTING);
        list.add(ServiceAuthConstants.USER_TYPE);
        list.add(ServiceAuthConstants.IDENTITY_SEPARATOR);

        //read Db settings name starting with "api.auth" to add additional provider specific settings
        List<Setting> settings = objectManager.find(Setting.class,
                SETTING.NAME, new Condition(ConditionType.LIKE, "api.auth%"));

        for (Setting setting : settings) {
            list.add(DynamicPropertyFactory.getInstance().getStringProperty(setting.getName(), null));
        }

        return list;
    }

    @Override
    public void reload() {
        if (!shouldRun()) {
            return;
        }

        try {
            StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());
            Request.Post(authUrl+"/reload").execute();
        } catch (IOException e) {
            log.info("Failed to reload auth service: {}", e.getMessage());
        }
    }

}
