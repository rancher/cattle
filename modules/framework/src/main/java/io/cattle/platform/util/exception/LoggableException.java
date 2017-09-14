package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public interface LoggableException {
    void log(Logger log);
}
