package io.cattle.platform.iaas.api.auth.integration;

import io.cattle.platform.api.auth.Identity;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.ApiError;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class IdentityNotFoundException extends ClientVisibleException {

    private static final long serialVersionUID = 4816163081210850808L;

    public IdentityNotFoundException(ApiError apiError) {
        super(apiError);
    }

    public IdentityNotFoundException(int status, String code, String message, String detail) {
        super(status, code, message, detail);
    }

    public IdentityNotFoundException(int status, String code) {
        super(status, code);
    }

    public IdentityNotFoundException(int status) {
        super(status);
    }
    public IdentityNotFoundException(Identity identity){
        super(ResponseCodes.NOT_FOUND, "identityNotFound", "Identity: " + identity.getId() + " not found.",
                "This maybe a result of an invalid identity, or the backing authentication system has deleted this identity.");
    }
}
