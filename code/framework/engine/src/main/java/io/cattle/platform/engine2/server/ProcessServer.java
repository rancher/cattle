package io.cattle.platform.engine2.server;

import io.cattle.platform.engine.server.ProcessInstanceExecutor;
import io.cattle.platform.engine2.model.ProcessInstance;
import io.cattle.platform.engine2.model.ProcessReference;
import io.cattle.platform.engine2.model.Trigger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessServer implements ProcessInstanceExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessServer.class);

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(50);
    @Inject
    ProcessInstanceExecutor processInstanceExecutor;

    Map<String, Queue<Long>> queuedByResource = new HashMap<>();
    Map<Long, ProcessReference> refs = new HashMap<>();
    Map<String, ProcessInstance> inflightByResource = new HashMap<>();
    List<Trigger> triggers = new ArrayList<>();

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
        executor.submit(instance);
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
                executor.submit(newInstance);
            }
        } else {
            Date date = instance.getRunAfter();
            long after = date == null ? 0 : date.getTime() - System.currentTimeMillis();
            if (after < 0) {
                after = 0;
            }
            System.err.println("AFTER!!! " + after);
            executor.schedule(instance, after, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isInPartition(ProcessReference ref) {
        return true;
    }

    @Override
    public io.cattle.platform.engine.process.ProcessInstance execute(long processId) {
        return processInstanceExecutor.execute(processId);
    }

}