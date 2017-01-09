package io.cattle.platform.engine.server;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

public class ProcessInstanceReference extends NoExceptionRunnable implements Comparable<Runnable>, Runnable {

    long processId;
    int priority;
    String name;
    ProcessInstanceExecutor executor;
    boolean event;

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Runnable left) {
        if (left instanceof ProcessInstanceReference) {
            ProcessInstanceReference o = (ProcessInstanceReference)left;
            int result = Integer.compare(priority, o.priority);
            return result == 0 ? Long.compare(processId, o.processId) :
                -result;
        }
        return Integer.compare(this.hashCode(), left.hashCode());
    }

    public ProcessInstanceExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ProcessInstanceExecutor executor) {
        this.executor = executor;
    }

    @Override
    protected void doRun() throws Exception {
        executor.execute(processId);
    }

    public boolean isEvent() {
        return event;
    }

    public void setEvent(boolean event) {
        this.event = event;
    }

}
