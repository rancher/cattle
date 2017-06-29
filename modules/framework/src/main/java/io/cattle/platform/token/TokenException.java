package io.cattle.platform.token;

public class TokenException extends Exception {

    private static final long serialVersionUID = -7844479344240139887L;

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenException(Throwable cause) {
        super(cause);
    }

}
