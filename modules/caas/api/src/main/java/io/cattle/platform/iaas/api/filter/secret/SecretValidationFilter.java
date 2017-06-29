package io.cattle.platform.iaas.api.filter.secret;

import io.cattle.platform.core.model.Secret;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import org.apache.commons.codec.binary.Base64;

public class SecretValidationFilter extends AbstractValidationFilter {

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Secret s = request.proxyRequestObject(Secret.class);
        if (!Base64.isBase64(s.getValue())) {
            throw new ValidationErrorException("InvalidBase64", "value", "Invalid base64 content");

        };
        Base64.decodeBase64(s.getValue());
        return super.create(type, request, next);
    }

}
