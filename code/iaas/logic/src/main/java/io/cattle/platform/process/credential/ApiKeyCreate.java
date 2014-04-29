package io.cattle.platform.process.credential;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.security.SecureRandom;

import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicStringProperty;

@Named
public class ApiKeyCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    public static final DynamicStringProperty BAD_CHARACTERS = ArchaiusUtil.getString("process.credential.create.bad.characters");
    public static final String API_KEY = "apiKey";

    final SecureRandom random = new SecureRandom();

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Credential credential = (Credential)state.getResource();

        if ( ! API_KEY.equals(credential.getKind()) ) {
            return null;
        }

        String publicValue = credential.getPublicValue();
        String secretValue = credential.getSecretValue();

        if ( publicValue == null ) {
            String[] keys = generateKeys();
            publicValue = keys[0];
            secretValue = keys[1];
        }

        /* Don't pass back secret value because it will be logged and that's not good */
        objectManager.setFields(credential, CREDENTIAL.SECRET_VALUE, secretValue);

        return new HandlerResult(
                CREDENTIAL.PUBLIC_VALUE, publicValue,
                "_secretHash", secretValue.hashCode()
            );
    }

    protected String[] generateKeys() {
        byte[] accessKey = new byte[10];
        byte[] secretKey = new byte[128];

        random.nextBytes(accessKey);
        random.nextBytes(secretKey);

        String accessKeyString = Hex.encodeHexString(accessKey);
        String secretKeyString = Base64.encodeBase64String(secretKey).replaceAll(BAD_CHARACTERS.get(), "");

        if ( secretKeyString.length() < 40 ) {
            /* Wow, this is terribly bad luck */
            throw new IllegalStateException("Failed to create secretKey due to not enough good characters");
        }

        return new String[] { accessKeyString.substring(0,20).toUpperCase(), secretKeyString.substring(0, 40) };
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "credential.create" };
    }

}
