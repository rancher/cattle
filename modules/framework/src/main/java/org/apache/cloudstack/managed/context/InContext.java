package org.apache.cloudstack.managed.context;

public interface InContext extends Runnable {

    @Override
    default void run() {
        new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                inContext();
            }
        }.run();
    }

    void inContext();

}
