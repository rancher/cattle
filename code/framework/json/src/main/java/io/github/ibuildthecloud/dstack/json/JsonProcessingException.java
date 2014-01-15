package io.github.ibuildthecloud.dstack.json;

public class JsonProcessingException extends RuntimeException {

    private static final long serialVersionUID = 7858379344964510826L;

    public JsonProcessingException() {
        super();
    }

    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonProcessingException(String message) {
        super(message);
    }

    public JsonProcessingException(Throwable cause) {
        super(cause);
    }

}
