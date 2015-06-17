package io.cattle.platform.process.credential;

import static io.cattle.platform.core.model.tables.CredentialTable.*;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.encryption.EncryptionConstants;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ApiKeyCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    TransformationService transformationService;

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

            if (getsHashed()) {
                secretValue = transformationService.transform(keys[1], EncryptionConstants.HASH);
            } else {
                secretValue = keys[1];
            }
        }

        if (secretValue == null) {
            return null;
        } else {
            /*
             * Don't pass back secret value because it will be logged and that's not
             * good
             */
            objectManager.setFields(credential, CREDENTIAL.SECRET_VALUE, secretValue);

            return new HandlerResult(CREDENTIAL.PUBLIC_VALUE, publicValue, "_secretHash", secretValue.hashCode());
        }
    }

    protected String getCredentialType() {
        return CredentialConstants.KIND_API_KEY;
    }

    protected boolean getsHashed() {
        return true;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "credential.create" };
    }

}
