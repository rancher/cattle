package io.cattle.platform.register.api;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.register.util.RegisterConstants;
import io.cattle.platform.register.util.RegistrationToken;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class RegistrationTokenOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if ( ! (original instanceof Credential) ) {
            return converted;
        }

        Credential cred = (Credential)original;

        String accessKey = cred.getPublicValue();
        String secretKey = cred.getSecretValue();

        if ( accessKey != null && secretKey != null ) {
            converted.getFields().put("token", RegistrationToken.createToken(accessKey, secretKey));
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
