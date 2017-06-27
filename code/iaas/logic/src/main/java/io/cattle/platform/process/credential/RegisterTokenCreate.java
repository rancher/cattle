package io.cattle.platform.process.credential;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.register.util.RegisterConstants;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

public class RegisterTokenCreate extends ApiKeyCreate {

    public RegisterTokenCreate(ObjectManager objectManager, TransformationService transformationService) {
        super(objectManager, transformationService);
    }

    @Override
    protected String getCredentialType() {
        return RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN;
    }

    @Override
    protected boolean getsHashed() {
        return false;
    }
}