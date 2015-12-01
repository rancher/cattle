package io.cattle.platform.iaas.api.auth.integration.ldap;

import javax.naming.NamingException;

public class UserLoginFailureException extends RuntimeException {
    private static final long serialVersionUID = 8982603952499071773L;
    private String username = null;
    public UserLoginFailureException() {
    }

    public UserLoginFailureException(String message) {
        super(message);
    }

    public UserLoginFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserLoginFailureException(Throwable cause) {
        super(cause);
    }

    public UserLoginFailureException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UserLoginFailureException(String s, NamingException e, String username) {
        super(s, e);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
