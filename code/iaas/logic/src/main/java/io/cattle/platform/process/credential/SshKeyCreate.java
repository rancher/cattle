package io.cattle.platform.process.credential;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.ssh.common.SshKeyGen;

import javax.inject.Named;

@Named
public class SshKeyCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Override
    public String[] getProcessNames() {
        return new String[] { "credential.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Credential cred = (Credential) state.getResource();

        if (!CredentialConstants.KIND_SSH_KEY.equals(cred.getKind())) {
            return null;
        }

        if (cred.getPublicValue() != null) {
            return new HandlerResult(CREDENTIAL.PUBLIC_VALUE, cred.getPublicValue(), CREDENTIAL.SECRET_VALUE, cred.getSecretValue());
        }

        String[] key;
        try {
            key = SshKeyGen.generateKeys();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ssh key", e);
        }

        return new HandlerResult(CREDENTIAL.PUBLIC_VALUE, key[0], CREDENTIAL.SECRET_VALUE, key[1]);
    }

}
