package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class ServiceReconcileException extends IllegalStateException implements LoggableException {

    private static final long serialVersionUID = 3938340725641990348L;


    public ServiceReconcileException(String s) {
        super(s);
    }


    @Override
    public void log(Logger log) {
        log.info(this.getMessage());
    }
}
