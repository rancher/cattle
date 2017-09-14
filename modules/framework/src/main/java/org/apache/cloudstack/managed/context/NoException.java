package org.apache.cloudstack.managed.context;

public interface NoException extends Runnable {

    @Override
    default void run() {
        new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                NoException.this.doRun();;
            }
        }.run();
    }

    void doRun();

}
