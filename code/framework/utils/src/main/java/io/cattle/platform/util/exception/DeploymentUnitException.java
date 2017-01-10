package io.cattle.platform.util.exception;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class DeploymentUnitException extends IllegalStateException implements LoggableException {
    private static final long serialVersionUID = 4868400759427367403L;

    Object deploymentUnit;

    public DeploymentUnitException() {
        super();
    }

    public DeploymentUnitException(String message, Throwable cause, Object deploymentUnit) {
        super(message + ": " + getMessage(cause));
        this.deploymentUnit = deploymentUnit;
    }

    private static String getMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        if (StringUtils.isBlank(t.getMessage())) {
            return t.getClass().getSimpleName();
        }
        return t.getMessage();
    }

    public DeploymentUnitException(String message, Object deploymentUnit) {
        super(message);
        this.deploymentUnit = deploymentUnit;
    }

    public Object getDeploymentUnit() {
        return deploymentUnit;
    }

    @Override
    public void log(Logger log) {
        log.error(this.getMessage());
    }
}