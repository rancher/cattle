package io.cattle.platform.register.process;

import io.cattle.platform.process.credential.ApiKeyCreate;
import io.cattle.platform.register.util.RegisterConstants;

public class RegisterTokenCreate extends ApiKeyCreate {

    @Override
    protected String getCredentialType() {
        return RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN;
    }

}