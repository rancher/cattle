package io.cattle.platform.engine.server.impl;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessWaitException;
import io.cattle.platform.engine.server.ProcessInstanceExecutor;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.LoggableException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstanceDispatcherImpl implements ProcessInstanceExecutor {
    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceDispatcherImpl.class);

    @Inject
    ProcessManager processManager;

    @Override
    public ProcessInstance execute(final long processId) {
        ProcessInstance instance = processManager.loadProcess(processId);
        return execute(processId, instance, false);
    }

    @Override
    public ProcessInstance resume(ProcessInstance instance) {
        return execute(instance.getId(), instance, true);
    }

    protected ProcessInstance execute(long processId, ProcessInstance instance, boolean resume) {
        try {
            if (instance != null) {
                if (resume) {
                    instance.resume();
                } else {
                    instance.execute();
                }
            }
        } catch (ProcessNotFoundException e) {
            log.debug("Failed to find process for id [{}]", processId);
        } catch (ProcessInstanceException e) {
            if (e.getExitReason().isError()) {
                log.error("Process [{}:{}] on [{}] failed, exit [{}] : {}", instance.getName(), processId, instance.getResourceId(), e
                        .getExitReason(), e.getMessage());
            }
        } catch (TimeoutException e) {
            log.info("Timeout on process [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e
                    .getMessage());
        } catch (ProcessWaitException e) {
            log.debug("Process wait [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e.getMessage());
            throw e;
        } catch (ProcessCancelException e) {
            log.debug("Process canceled [{}:{}] on [{}] : {}", instance.getName(), processId, instance.getResourceId(), e.getMessage());
        } catch (Throwable e) {
            if (e instanceof LoggableException) {
                ((LoggableException) e).log(log);
            } else {
                Throwable cause = ExceptionUtils.getRootCause(e);
                if (cause instanceof ProcessCancelException) {
                    log.error("Unknown exception running process [{}:{}] on [{}], canceled by [{}]",
                            instance == null ? null : instance.getName(), processId,
                            instance == null ? null : instance.getResourceId(), cause.getMessage());
                } else {
                    log.error("Unknown exception running process [{}:{}] on [{}]",
                            instance == null ? null : instance.getName(), processId,
                            instance == null ? null : instance.getResourceId(), e);
                }
            }
        }

        return instance;
    }

}