package io.cattle.platform.iaas.api.auth.integration.ldap;

public class ServiceContextRetrievalException extends RuntimeException {

    private static final long serialVersionUID = 953854846474061817L;

    public ServiceContextRetrievalException() {
    }

    public ServiceContextRetrievalException(String message) {
        super(message);
    }

    public ServiceContextRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceContextRetrievalException(Throwable cause) {
        super(cause);
    }

    public ServiceContextRetrievalException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
