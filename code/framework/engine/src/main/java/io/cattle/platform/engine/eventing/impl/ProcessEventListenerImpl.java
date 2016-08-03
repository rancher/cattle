package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.LoggableException;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.netflix.config.DynamicLongProperty;

public class ProcessEventListenerImpl implements ProcessEventListener {
    private static final DynamicLongProperty RETRY_MAX_WAIT = ArchaiusUtil.getLong("process.retry_max_wait.millis");

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListenerImpl.class);

    private static Counter EVENT = MetricsUtil.getRegistry().counter("process_execution.event");
    private static Counter CANCELED = MetricsUtil.getRegistry().counter("process_execution.cancel");
    private static Counter DONE = MetricsUtil.getRegistry().counter("process_execution.done");
    private static Counter NOT_FOUND = MetricsUtil.getRegistry().counter("process_execution.not_found");
    private static Counter EXCEPTION = MetricsUtil.getRegistry().counter("process_execution.unknown_exception");
    private static Counter TIMEOUT = MetricsUtil.getRegistry().counter("process_execution.timeout");

    @Inject
    ProcessRecordDao processRecordDao;

    ProcessManager processManager;
    ProcessServer processServer;
    Map<ExitReason, Counter> counters = new HashMap<ExitReason, Counter>();

    @Override
    public void processExecute(Event event) {
        if (event.getResourceId() == null)
            return;

        processExecute(new Long(event.getResourceId()));
    }

    public void processExecute(long processId) {
        if (!ProcessEngineUtils.enabled()) {
            return;
        }

        if (shouldWaitLonger(processId)) {
            return;
        }

        EVENT.inc();

        boolean runRemaining = false;
        ProcessInstance instance = null;
        try {
            instance = processManager.loadProcess(processId);
            instance.execute();
            runRemaining = true;
            DONE.inc();
        } catch (ProcessNotFoundException e) {
            NOT_FOUND.inc();
            log.debug("Failed to find process for id [{}]", processId);
        } catch (ProcessInstanceException e) {
            counters.get(e.getExitReason()).inc();
            if (e.getExitReason().isError()) {
                log.error("Process [{}:{}] on [{}] failed, exit [{}] : {}", instance.getName(), processId, instance.getResourceId(), e
                        .getExitReason(), e.getMessage());
            }
        } catch (TimeoutException e) {
            TIMEOUT.inc();
            log.info("Communication timeout on process [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e
                    .getMessage());
        } catch (ProcessCancelException e) {
            CANCELED.inc();
            log.debug("Process canceled [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e.getMessage());

        } catch (Throwable e) {
            if (e instanceof LoggableException) {
                EXCEPTION.inc();
                ((LoggableException) e).log(log);
            } else {
                Throwable cause = ExceptionUtils.getRootCause(e);
                if (cause instanceof ProcessCancelException) {
                    CANCELED.inc();
                    log.error("Unknown exception running process [{}:{}] on [{}], canceled by [{}]",
                            instance == null ? null : instance.getName(), processId,
                            instance == null ? null : instance.getResourceId(), cause.getMessage());
                } else {
                    EXCEPTION.inc();
                    log.error("Unknown exception running process [{}:{}] on [{}]",
                            instance == null ? null : instance.getName(), processId,
                            instance == null ? null : instance.getResourceId(), e);
                }
            }
        }

        if (runRemaining) {
            Long nextId = processServer.getRemainingTasks(processId);
            if (nextId != null) {
                processExecute(nextId);
            }
        }
    }

    private boolean shouldWaitLonger(long processId) {
        ProcessRecord record = processRecordDao.getRecord(processId);
        if (record == null || record.getEndTime() != null) {
            // is there any additional processing that happens or can we
            // even short circuit it here and be done with this event?
            // for now, assume additional processing occurs to get rid
            // of event
            return false;
        }
        int numPreviousAttempts = processRecordDao.getNumPreviousExecutions(processId);
        if (numPreviousAttempts < 2) {
            return false;
        }

        long now = System.currentTimeMillis();
        long lastExecution = processRecordDao.getLastExecutionTimestamp(processId);
        long maxWait = Math.min(RETRY_MAX_WAIT.get(), 15000L + (long)Math.pow(2,  numPreviousAttempts) / 2);
        return now < lastExecution + maxWait;
    }

    @PostConstruct
    public void init() {
        for (ExitReason e : ExitReason.values()) {
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
