package io.cattle.platform.process.credential;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

public class RegisterTokenCreate extends ApiKeyCreate {

    public RegisterTokenCreate(ObjectManager objectManager, TransformationService transformationService) {
        super(objectManager, transformationService);
    }

    @Override
    protected String getCredentialType() {
        return CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN;
    }

    @Override
    protected boolean getsHashed() {
        return false;
    }
}