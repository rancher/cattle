package io.github.ibuildthecloud.dstack.allocator.exception;

public class UnsupportedAllocation extends RuntimeException {

    private static final long serialVersionUID = -3580983723107948662L;

    public UnsupportedAllocation() {
        super();
    }

    public UnsupportedAllocation(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedAllocation(String message) {
        super(message);
    }

    public UnsupportedAllocation(Throwable cause) {
        super(cause);
    }

}
