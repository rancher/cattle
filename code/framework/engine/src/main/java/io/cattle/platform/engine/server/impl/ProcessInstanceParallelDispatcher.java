package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.eventing.impl.ProcessEventListenerImpl;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;

import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;

public class ProcessInstanceParallelDispatcher implements ProcessInstanceDispatcher {

    @Inject
    ProcessEventListenerImpl eventListener;

    @Inject
    @Named("ProcessExecutorServiceReplay")
    ThreadPoolExecutor executor;

    @Override
    public void execute(final Long id) {
        executor.submit(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                eventListener.processExecute(id);
            }
        });
    }

}