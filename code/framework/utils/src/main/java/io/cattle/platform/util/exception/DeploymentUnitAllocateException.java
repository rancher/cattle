package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class DeploymentUnitAllocateException extends DeploymentUnitException implements LoggableException {

    @Override
    public void log(Logger log) {
        log.info(this.getMessage());
    }

    public DeploymentUnitAllocateException(String message, Exception ex, Object du) {
        super(message, ex, du);
    }

}