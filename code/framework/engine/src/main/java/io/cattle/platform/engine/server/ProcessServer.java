package io.cattle.platform.engine.server;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.model.ProcessInstance;
import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.model.Trigger;

import java.util.ArrayList;
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

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessServer implements ProcessInstanceExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessServer.class);

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

    Map<String, Queue<Long>> queuedByResource = new HashMap<>();
    Map<Long, ProcessReference> refs = new HashMap<>();
    Map<String, ProcessInstance> inflightByResource = new HashMap<>();
    List<Trigger> triggers = new ArrayList<>();

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
        if (inflightByResource.containsKey(resourceKey)) {
            queueByResource(resourceKey, ref);
            return;
        }

        ProcessInstance instance = new ProcessInstance(ref, this, getPriority(ref));
        inflightByResource.put(resourceKey, instance);
        submitToExecutor(instance);
    }

    protected Integer getPriority(ProcessReference ref) {
        return 0;
    }

    protected void queueByResource(String resourceKey, ProcessReference ref) {
        Queue<Long> queue = queuedByResource.get(resourceKey);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            queuedByResource.put(resourceKey, queue);
        }
        queue.add(ref.getId());
    }

    public synchronized void trigger(ProcessInstance instance) {
        for (Trigger trigger : triggers) {
            try {
                trigger.trigger(instance);
            } catch (Throwable t) {
                log.error("Exception while running trigger [{}] on [{}]", trigger, instance.getRef(), t);
            }
        }
    }

    protected void submitToExecutor(ProcessInstance instance) {
        if (isBlocking(instance)) {
            blockingExecutor.execute(instance);
        } else {
            nonBlockingExecutor.execute(instance);
        }
    }

    public synchronized void done(ProcessInstance instance) {
        ProcessReference ref = instance.getRef();
        if (instance.isDone()) {
            String resourceKey = ref.getResourceKey();
            refs.remove(ref.getId());
            Queue<Long> queue = queuedByResource.get(resourceKey);
            if (queue == null) {
                inflightByResource.remove(resourceKey);
            } else {
                ProcessReference newRef = refs.get(queue.remove());
                if (queue.size() == 0) {
                    queuedByResource.remove(ref.getResourceKey());
                }
                ProcessInstance newInstance = new ProcessInstance(newRef, this, -1000);
                inflightByResource.put(resourceKey, newInstance);
                submitToExecutor(newInstance);
            }
        } else {
            Date date = instance.getRunAfter();
            long after = date == null ? 0 : date.getTime() - System.currentTimeMillis();
            if (after < 0) {
                after = 0;
            }
            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    submitToExecutor(instance);
                }
            }, after, TimeUnit.MILLISECONDS);
        }
    }

    protected boolean isBlocking(ProcessInstance instance) {
        ProcessReference ref = instance.getRef();
        if (ArchaiusUtil.getBoolean("process." + ref.getName().split("[.]")[0] + ".blocking").get()) {
            return true;
        }
        return ArchaiusUtil.getBoolean("process." + ref.getName() + ".blocking").get();
    }

    public boolean isInPartition(ProcessReference ref) {
        Long id = ref.getAccountId();
        if (id == null) {
            id = 0L;
        }
        Pair<Integer, Integer> countAndIndex = cluster.getCountAndIndex();
        int count = countAndIndex.getLeft();
        int index = countAndIndex.getRight();
        if (count <= 1) {
            return true;
        }
        return (id % count) == index;
    }

    @Override
    public io.cattle.platform.engine.process.ProcessInstance execute(long processId) {
        return processInstanceExecutor.execute(processId);
    }

}