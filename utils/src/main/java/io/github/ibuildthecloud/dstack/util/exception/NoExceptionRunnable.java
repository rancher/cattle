package io.github.ibuildthecloud.dstack.util.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NoExceptionRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NoExceptionRunnable.class);

    @Override
    public final void run() {
        try {
            doRun();
        } catch (Throwable t) {
            log.error("Uncaught exception", t);
        }
    }

    public abstract void doRun() throws Exception;

}