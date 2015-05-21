package io.cattle.platform.process.credential;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.security.SecureRandom;

import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicStringProperty;

@Named
public class ApiKeyCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Credential credential = (Credential) state.getResource();

        if (!getCredentialType().equals(credential.getKind())) {
            return null;
        }

        String publicValue = credential.getPublicValue();
        String secretValue = credential.getSecretValue();

        if (publicValue == null) {
            String[] keys = ApiKeyFilter.generateKeys();
            publicValue = keys[0];
            secretValue = keys[1];
        }

        /*
         * Don't pass back secret value because it will be logged and that's not
         * good
         */
        objectManager.setFields(credential, CREDENTIAL.SECRET_VALUE, secretValue);

        return new HandlerResult(CREDENTIAL.PUBLIC_VALUE, publicValue, "_secretHash", secretValue.hashCode());
    }

    protected String getCredentialType() {
        return CredentialConstants.KIND_API_KEY;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "credential.create" };
    }

}
