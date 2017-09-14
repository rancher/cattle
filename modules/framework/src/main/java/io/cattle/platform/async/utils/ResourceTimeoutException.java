package io.cattle.platform.async.utils;

public class ResourceTimeoutException extends TimeoutException {

    private static final long serialVersionUID = -3183831604305875594L;

    Object resource;

    public ResourceTimeoutException(Object resource, String message) {
        super(message);
        this.resource = resource;
    }

    public Object getResource() {
        return resource;
    }

}