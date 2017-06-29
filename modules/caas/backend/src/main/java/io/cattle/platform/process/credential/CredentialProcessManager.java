package io.cattle.platform.process.credential;

import static io.cattle.platform.core.model.tables.CredentialTable.*;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.encryption.EncryptionConstants;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.Set;

public class CredentialProcessManager {

    private static final Set<String> CREATE_KIND = CollectionUtils.set(
            CredentialConstants.KIND_API_KEY,
            CredentialConstants.KIND_AGENT_API_KEY,
            CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN);

    ObjectManager objectManager;
    TransformationService transformationService;

    public CredentialProcessManager(ObjectManager objectManager, TransformationService transformationService) {
        this.objectManager = objectManager;
        this.transformationService = transformationService;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Credential credential = (Credential) state.getResource();

        if (!CREATE_KIND.contains(credential.getKind())) {
            return null;
        }

        String publicValue = credential.getPublicValue();
        String secretValue = credential.getSecretValue();
        if (publicValue == null) {
            String[] keys = ApiKeyFilter.generateKeys();
            publicValue = keys[0];

            if (credential.getKind().equals(CredentialConstants.KIND_API_KEY)) {
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

}
