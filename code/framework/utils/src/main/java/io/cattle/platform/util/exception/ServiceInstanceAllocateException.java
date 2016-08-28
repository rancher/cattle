package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class ServiceInstanceAllocateException extends InstanceException implements LoggableException {

    private static final long serialVersionUID = -5376205462062705074L;

    @Override
    public void log(Logger log) {
        log.info(this.getMessage());
    }

    public ServiceInstanceAllocateException(String message, Exception ex, Object instance) {
        super(message, ex, instance);
    }

}
