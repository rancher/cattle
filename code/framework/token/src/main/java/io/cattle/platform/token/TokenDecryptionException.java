package io.cattle.platform.token;

public class TokenDecryptionException extends TokenException {

    private static final long serialVersionUID = 7353678723501410607L;

    public TokenDecryptionException(String message) {
        super(message);
    }

    public TokenDecryptionException(Throwable cause) {
        super(cause);
    }

    public TokenDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
