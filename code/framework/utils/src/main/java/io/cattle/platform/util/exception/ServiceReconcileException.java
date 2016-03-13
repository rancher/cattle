package io.cattle.platform.util.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceReconcileException extends IllegalStateException implements LoggableException {
    private static final Logger log = LoggerFactory.getLogger(ServiceReconcileException.class);

    private static final long serialVersionUID = 3938340725641990348L;


    public ServiceReconcileException(String s) {
        super(s);
    }


    @Override
    public void log() {
        log.info(this.getMessage());
    }
}
