package io.cattle.platform.engine.model;

import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.impl.ProcessWaitException;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.util.type.Priority;

import java.util.Date;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class ProcessInstanceWrapper extends NoExceptionRunnable implements Runnable, Comparable<ProcessInstanceWrapper>, Priority {

    private static Logger log = LoggerFactory.getLogger(ProcessInstanceWrapper.class);

    int priority;
    ProcessServer processServer;
    ProcessReference ref;
    io.cattle.platform.engine.process.ProcessInstance instance;
    boolean done;
    boolean waiting;
    ListenableFuture<?> future;

    public ProcessInstanceWrapper(ProcessReference ref, ProcessServer processServer, int priority) {
        this.priority = priority;
        this.processServer = processServer;
        this.ref = ref;
    }

    public ProcessReference getRef() {
        return ref;
    }

    public Date getRunAfter() {
        if (instance == null) {
            return null;
        }
        if (instance.getProcessRecord() == null) {
            return null;
        }
        return instance.getProcessRecord().getRunAfter();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(ProcessInstanceWrapper o) {
        int i = Integer.compare(priority, o.priority);
        if (i == 0) {
            return Integer.compare(hashCode(), o.hashCode());
        }
        return i;
    }

    @Override
    protected void doRun() {
        boolean resume = instance != null && future != null && waiting;
        done = false;
        waiting = false;
        future = null;
        if (!resume) {
            instance = null;
        }

        try {
            if (!processServer.isInPartition(getRef())) {
                done = true;
                return;
            }

            instance = resume ? processServer.resume(instance) : processServer.execute(ref.getId());
            if (instance == null) {
                done = true;
            } else {
                done = instance.getExitReason() != null && instance.getExitReason().isTerminating();
            }
        } catch (ProcessNotFoundException e) {
            done = true;
        } catch (ProcessWaitException e) {
            waiting = true;
            instance = e.getProcessInstance();
            future = e.getFuture();
        } catch (Throwable t) {
            log.error("Failed to run process [{}]", ref.getId(), t);
        } finally {
            processServer.done(this);
        }
    }

    public boolean isDone() {
        return done;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public Object getResource() {
        return instance == null ? null : instance.getResource();
    }

    public void onReady(Runnable runnable) {
        if (future == null) {
            throw new IllegalStateException("Can not register onResume for non-waiting instance [" + getRef() + "]");
        }
        future.addListener(runnable, processServer.getExecutor());
    }
}
