package io.cattle.platform.engine2.model;

import io.cattle.platform.engine2.server.ProcessServer;
import io.cattle.platform.util.type.Priority;

import java.util.Date;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstance extends ManagedContextRunnable implements Runnable, Comparable<ProcessInstance>, Priority {

    private static Logger log = LoggerFactory.getLogger(ProcessInstance.class);

    int priority;
    ProcessServer processServer;
    ProcessReference ref;
    io.cattle.platform.engine.process.ProcessInstance instance;
    boolean done;

    public ProcessInstance(ProcessReference ref, ProcessServer processServer, int priority) {
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
    public int compareTo(ProcessInstance o) {
        int i = Integer.compare(priority, o.priority);
        if (i == 0) {
            return Integer.compare(hashCode(), o.hashCode());
        }
        return i;
    }

    @Override
    protected void runInContext() {
        System.err.println("RUNNING!!! " + getRef().getResourceKey() + " " + getRef().getName());
        instance = null;
        done = false;
        try {
            if (!processServer.isInPartition(getRef())) {
                done = true;
                return;
            }

            instance = processServer.execute(ref.getId());
            if (instance == null) {
                done = true;
            } else {
                done = instance.getExitReason() != null && instance.getExitReason().isTerminating();
            }
        } catch (Throwable t) {
            log.error("Failed to run process [{}]", ref.getId(), t);
        } finally {
            if (done) {
                processServer.trigger(this);
            }
            processServer.done(this);
        }
    }

    public boolean isDone() {
        return done;
    }
}
