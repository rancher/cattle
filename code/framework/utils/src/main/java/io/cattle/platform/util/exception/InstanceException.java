package io.cattle.platform.util.exception;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class InstanceException extends IllegalStateException implements LoggableException {
    private static final long serialVersionUID = 4868400759427367403L;

    Object instance;

    public InstanceException() {
        super();
    }

    public InstanceException(String message, Throwable cause, Object instance) {
        super(message + ": " + getMessage(cause));
        this.instance = instance;
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

    public InstanceException(String message, Object instance) {
        super(message);
        this.instance = instance;
    }

    public Object getInstance() {
        return instance;
    }

    @Override
    public void log(Logger log) {
        log.error(this.getMessage());
    }
}
