package io.cattle.platform.util.exception;

public class UnreachableException extends RuntimeException {

    private static final long serialVersionUID = 698044465331118869L;

    public UnreachableException() {
        super("This line of code should not have been reached, this is a bug");
    }

}
