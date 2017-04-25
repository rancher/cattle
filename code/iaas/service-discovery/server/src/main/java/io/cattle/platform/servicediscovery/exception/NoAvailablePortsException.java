package io.cattle.platform.servicediscovery.exception;

public class NoAvailablePortsException extends RuntimeException { 

    private static final long serialVersionUID = -242204355882649316L;

    public NoAvailablePortsException() {
        super();
    }
    
    public NoAvailablePortsException(String message) {
        super(message);
    }
}
