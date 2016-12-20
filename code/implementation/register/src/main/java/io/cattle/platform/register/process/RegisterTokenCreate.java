package io.cattle.platform.register.process;

import io.cattle.platform.process.credential.ApiKeyCreate;
import io.cattle.platform.register.util.RegisterConstants;

import javax.inject.Named;

@Named
public class RegisterTokenCreate extends ApiKeyCreate {

    @Override
    protected String getCredentialType() {
        return RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN;
    }

    @Override
    protected boolean getsHashed() {
        return false;
    }
}