package io.cattle.platform.api.apikey;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

public class ApiKeyFilter extends AbstractValidationFilter {

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Credential cred = request.proxyRequestObject(Credential.class);
        if (cred.getPublicValue() == null) {
            String[] keys = SecurityConstants.generateKeys();
            cred.setPublicValue(keys[0]);
            cred.setSecretValue(keys[1]);
        }
        String clearSecret = cred.getSecretValue();
        if (clearSecret != null) {
            cred.setSecretValue(ApiContext.getContext().getTransformationService().transform(clearSecret, "HASH"));
        }
        cred = (Credential) super.create(type, request, next);
        cred.setSecretValue(clearSecret);
        return cred;
    }


}