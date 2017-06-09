package io.cattle.platform.engine.server;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.model.ProcessInstanceWrapper;
import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.model.Trigger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

public class ProcessServer implements ProcessInstanceExecutor {

    @Inject
    ScheduledExecutorService scheduledExecutor;
    @Inject @Named("ProcessBlockingExecutorService")
    ExecutorService blockingExecutor;
    @Inject @Named("ProcessNonBlockingExecutorService")
    ExecutorService nonBlockingExecutor;
    @Inject
    ProcessInstanceExecutor processInstanceExecutor;
    @Inject
    ProcessManager repository;
    @Inject
    Cluster cluster;
    @Inject
    List<Trigger> triggers;

    Map<String, Queue<Long>> queuedByResource = new HashMap<>();
    Map<Long, ProcessReference> refs = new HashMap<>();
    Map<String, ProcessInstanceWrapper> inflightByResource = new HashMap<>();

    public void runOutstandingJobs() {
        for (ProcessReference ref : repository.pendingTasks()) {
            submit(ref);
        }
    }

    public synchronized void submit(ProcessReference ref) {
        if (ref == null) {
            return;
        }

        if (!isInPartition(ref)) {
            return;
        }

        if (refs.containsKey(ref.getId())) {
            return;
        }

        refs.put(ref.getId(), ref);

        String resourceKey = ref.getResourceKey();
        ProcessInstanceWrapper instance = new ProcessInstanceWrapper(ref, this, getPriority(ref));
        submitToExecutor(resourceKey, instance);
    }

    protected Integer getPriority(ProcessReference ref) {
        return 0;
    }

    protected boolean isResourceInFlight(String resourceKey, ProcessInstanceWrapper ignore) {
        ProcessInstanceWrapper existing = inflightByResource.get(resourceKey);
        if (existing == null) {
            return false;
        }
        return existing != ignore;
    }

    protected void queueByResource(String resourceKey, ProcessReference ref) {
        Queue<Long> queue = queuedByResource.get(resourceKey);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            queuedByResource.put(resourceKey, queue);
        }
        queue.add(ref.getId());
    }

    protected synchronized void submitToExecutor(String resourceKey, ProcessInstanceWrapper instance) {
        if (isResourceInFlight(resourceKey, instance)) {
            queueByResource(resourceKey, instance.getRef());
        } else if (isBlocking(instance)) {
            inflightByResource.put(resourceKey, instance);
            blockingExecutor.execute(instance);
        } else {
            inflightByResource.put(resourceKey, instance);
            nonBlockingExecutor.execute(instance);
        }
    }

    public synchronized void done(ProcessInstanceWrapper instance) {
        ProcessReference ref = instance.getRef();
        String resourceKey = ref.getResourceKey();

        if (instance.isDone()) {
            inflightByResource.remove(resourceKey);
            refs.remove(ref.getId());
            Queue<Long> queue = queuedByResource.get(resourceKey);
            if (queue != null) {
                ProcessReference newRef = refs.get(queue.remove());
                if (queue.size() == 0) {
                    queuedByResource.remove(ref.getResourceKey());
                }
                ProcessInstanceWrapper newInstance = new ProcessInstanceWrapper(newRef, this, -1000);
                submitToExecutor(resourceKey, newInstance);
            }
        } else if (instance.isWaiting()) {
            // Notice that inflightByResource is not cleared on waiting processes, this is to simulate the old behavior of blocking and
            // waiting for a timeout before running another process.  Maybe this can be revisited after more refactoring.
            instance.onReady(() -> submitToExecutor(resourceKey, instance));
        } else {
            inflightByResource.remove(resourceKey);
            Date date = instance.getRunAfter();
            long after = date == null ? 0 : date.getTime() - System.currentTimeMillis();
            if (after < 0) {
                after = 0;
            }
            scheduledExecutor.schedule(() -> submitToExecutor(resourceKey, instance),
                    after, TimeUnit.MILLISECONDS);
        }
    }

    protected boolean isBlocking(ProcessInstanceWrapper instance) {
        ProcessReference ref = instance.getRef();
        if (ArchaiusUtil.getBoolean("process." + ref.getName().split("[.]")[0] + ".blocking").get()) {
            return true;
        }
        return ArchaiusUtil.getBoolean("process." + ref.getName() + ".blocking").get();
    }

    public boolean isInPartition(ProcessReference ref) {
        return cluster.isInPartition(ref.getAccountId());
    }

    @Override
    public io.cattle.platform.engine.process.ProcessInstance execute(long processId) {
        return processInstanceExecutor.execute(processId);
    }

    @Override
    public io.cattle.platform.engine.process.ProcessInstance resume(io.cattle.platform.engine.process.ProcessInstance instance) {
        return processInstanceExecutor.resume(instance);
    }

    public ExecutorService getExecutor() {
        return nonBlockingExecutor;
    }

}