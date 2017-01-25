package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class SecretsApiLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty SECRETS_BINARY = ArchaiusUtil.getString("secrets.api.service.executable");
    private static final DynamicBooleanProperty LAUNCH_SECRETS = ArchaiusUtil.getBoolean("secrets.api.execute");
    private static final DynamicStringProperty SECRETS_PATH = ArchaiusUtil.getString("secrets.api.local.key.path");

    @Inject
    JsonMapper jsonMapper;
    @Inject
    DataDao dataDao;

    @Override
    protected boolean shouldRun() {
        return LAUNCH_SECRETS.get();
    }

    @Override
    protected String binaryPath() {
        return SECRETS_BINARY.get();
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        String key = dataDao.getOrCreate("api.local.key", false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                SecureRandom random = new SecureRandom();
                byte[] bytes = new byte[32];
                random.nextBytes(bytes);
                return Hex.encodeHexString(bytes);
            }
        });
        File keyFile = new File(SECRETS_PATH.get(), SecretsService.SECRETS_KEY_NAME.get());
        try(FileOutputStream fos = new FileOutputStream(keyFile)) {
            try {
                IOUtils.write(Hex.decodeHex(key.toCharArray()), fos);
            } catch (DecoderException e) {
                throw new IOException(e);
            }
        }

        List<String> args = pb.command();
        args.add("server");
        args.add("--enc-key-path");
        args.add(SECRETS_PATH.get());
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
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
