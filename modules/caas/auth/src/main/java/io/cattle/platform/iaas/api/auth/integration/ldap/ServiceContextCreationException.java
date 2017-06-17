package io.cattle.platform.iaas.api.auth.integration.ldap;

public class ServiceContextCreationException extends RuntimeException{

    private static final long serialVersionUID = 563403741374310236L;

    public ServiceContextCreationException() {
    }

    public ServiceContextCreationException(String message) {
        super(message);
    }

    public ServiceContextCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceContextCreationException(Throwable cause) {
        super(cause);
    }

    public ServiceContextCreationException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
