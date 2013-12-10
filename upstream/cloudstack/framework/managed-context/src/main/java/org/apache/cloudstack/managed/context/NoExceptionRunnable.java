package org.apache.cloudstack.managed.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NoExceptionRunnable extends ManagedContextRunnable {

    private static final Logger log = LoggerFactory.getLogger(NoExceptionRunnable.class);

    @Override
    protected void runInContext() {
        try {
            doRun();
        } catch (Throwable t) {
            log.error("Uncaught exception", t);
        }
    }

    protected abstract void doRun() throws Exception;

}
