package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.metrics.util.MetricsUtil;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;

public class ProcessEventListenerImpl implements ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListenerImpl.class);

    private static Counter EVENT = MetricsUtil.getRegistry().counter("process_execution.event");
    private static Counter DONE = MetricsUtil.getRegistry().counter("process_execution.done");
    private static Counter NOT_FOUND = MetricsUtil.getRegistry().counter("process_execution.not_found");
    private static Counter EXCEPTION = MetricsUtil.getRegistry().counter("process_execution.unknown_exception");
    private static Counter TIMEOUT = MetricsUtil.getRegistry().counter("process_execution.timeout");

    ProcessManager processManager;
    ProcessServer processServer;
    Map<ExitReason,Counter> counters = new HashMap<ExitReason, Counter>();

    @Override
    public void processExecute(Event event) {
        if ( event.getResourceId() == null )
            return;

        EVENT.inc();

        long processId = new Long(event.getResourceId());
        boolean runRemaining = false;
        ProcessInstance instance = null;
        try {
            instance = processManager.loadProcess(processId);
            instance.execute();
            runRemaining = true;
            DONE.inc();
        } catch ( ProcessNotFoundException e ) {
            NOT_FOUND.inc();
            log.debug("Failed to find process for id [{}]", event.getResourceId());
        } catch ( ProcessInstanceException e ) {
            counters.get(e.getExitReason()).inc();
            switch (e.getExitReason()) {
            case PROCESS_ALREADY_IN_PROGRESS:
            case RESOURCE_BUSY:
                break;
            default:
                log.error("Process [{}:{}] on [{}] failed, exit [{}] : {}", instance.getName(), event.getResourceId(),
                        instance.getResourceId(), e.getExitReason(), e.getMessage());
            }
        } catch ( TimeoutException e ) {
            TIMEOUT.inc();
            log.info("Communication timeout on process [{}:{}] on [{}] : {}", instance.getName(), event.getResourceId(),
                    instance.getResourceId(), e.getMessage());
        } catch ( RuntimeException e ) {
            EXCEPTION.inc();
            log.error("Unknown exception running process [{}:{}] on [{}]", instance == null ? null : instance.getName(),
                    event.getResourceId(), instance == null ? null : instance.getResourceId(), e);
        }

        if ( runRemaining ) {
            processServer.runRemainingTasks(processId);
        }
    }

    @PostConstruct
    public void init() {
        for ( ExitReason e : ExitReason.values() ) {
            switch (e) {
            case DONE:
            case ALREADY_DONE:
                break;
            default:
                counters.put(e, MetricsUtil.getRegistry().counter("process_execution." + e.toString().toLowerCase()));
            }
        }
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public ProcessServer getProcessServer() {
        return processServer;
    }

    @Inject
    public void setProcessServer(ProcessServer processServer) {
        this.processServer = processServer;
    }

}
