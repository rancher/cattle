package io.cattle.platform.engine.server.impl;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.LoggableException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;

public class ProcessInstanceDispatcherImpl implements ProcessInstanceDispatcher {
    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceDispatcherImpl.class);

    private static Counter EVENT = MetricsUtil.getRegistry().counter("process_execution.event");
    private static Counter CANCELED = MetricsUtil.getRegistry().counter("process_execution.cancel");
    private static Counter DONE = MetricsUtil.getRegistry().counter("process_execution.done");
    private static Counter NOT_FOUND = MetricsUtil.getRegistry().counter("process_execution.not_found");
    private static Counter EXCEPTION = MetricsUtil.getRegistry().counter("process_execution.unknown_exception");
    private static Counter TIMEOUT = MetricsUtil.getRegistry().counter("process_execution.timeout");

    @Inject @Named("ProcessExecutorService")
    ExecutorService executor;
    @Inject @Named("PriorityProcessExecutorService")
    ExecutorService priorityExecutor;
    @Inject
    ProcessRecordDao processRecordDao;
    @Inject
    LockManager lockManager;
    @Inject
    ProcessManager processManager;
    @Inject
    ProcessServer processServer;
    Map<ExitReason, Counter> counters = new HashMap<ExitReason, Counter>();

    @Override
    public void execute(final Long id, boolean priority) {
        if (!ProcessEngineUtils.enabled()) {
            return;
        }

        Runnable run = new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                lockManager.tryLock(new ProcessDispatchLock(id), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        processExecuteWithLock(id);
                    }
                });
            }
        };

        boolean submitted = false;
        if (priority) {
            try {
                priorityExecutor.execute(run);
                submitted = true;
            } catch (RejectedExecutionException e) {
                //
            }
        }
        if (!submitted) {
            executor.execute(run);
        }
    }

    public void processExecuteWithLock(long processId) {
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
            log.info("Timeout on process [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e
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
                processExecuteWithLock(nextId);
            }
        }
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

}