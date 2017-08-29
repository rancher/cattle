package io.cattle.platform.object.monitor.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.ResourceTimeoutException;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskOptions;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResourceMonitorImpl implements ResourceMonitor, AnnotatedEventListener, Task, TaskOptions {

    private static final DynamicLongProperty DEFAULT_WAIT = ArchaiusUtil.getLong("resource.monitor.default.wait.millis");

    ObjectManager objectManager;
    ConcurrentMap<String, List<Runnable>> waiters = new ConcurrentHashMap<>();
    IdFormatter idFormatter;

    public ResourceMonitorImpl(ObjectManager objectManager, IdFormatter idFormatter) {
        super();
        this.objectManager = objectManager;
        this.idFormatter = idFormatter;
    }

    @EventHandler
    public void stateChanged(Event event) {
        resourceChange(event);
    }

    @EventHandler
    public void resourceChange(Event event) {
        String key = key(event.getResourceType(), event.getResourceId());
        waiters.computeIfPresent(key, (waitKey, checkers) -> {
            checkers.forEach(Runnable::run);
            return checkers;
        });
    }

    protected String key(Object resourceType, Object resourceId) {
        resourceId = idFormatter.formatId(resourceType.toString(), resourceId);
        return String.format("%s:%s", resourceType, resourceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ListenableFuture<T> waitFor(T input, long timeout, String message, ResourcePredicate<T> predicate) {
        if (input == null || predicate == null) {
            return AsyncUtils.done(input);
        }

        SettableFuture<T> future = SettableFuture.create();
        String type = objectManager.getType(input);
        String kind = ObjectUtils.getKind(input);
        if (kind == null) {
            kind = type;
        }
        Object id = ObjectUtils.getId(input);

        if (type == null || id == null) {
            throw new IllegalArgumentException("Type and id are required got [" + type + "] [" + id + "]");
        }

        String printKey = key(kind, id);
        String key = key(type, id);
        long end = System.currentTimeMillis() + timeout;

        Runnable checker = () -> {
            T obj = objectManager.reload(input);
            if (predicate.evaluate(obj)) {
                future.set(obj);
                waiters.remove(key);
            } else if (System.currentTimeMillis() >= end) {
                future.setException(new ResourceTimeoutException(obj, "Waiting: " + message + " [" + printKey + "]"));
                waiters.remove(key);
            }
        };

        try {
            waiters.compute(key, (k, runnables) -> {
                if (runnables == null) {
                    return new CopyOnWriteArrayList<>(new Runnable[]{checker});
                }
                runnables.add(checker);
                return runnables;
            });
            return future;
        } finally {
            checker.run();
        }
    }

    @Override
    public <T> ListenableFuture<T> waitFor(T obj, String message, ResourcePredicate<T> predicate) {
        return waitFor(obj, DEFAULT_WAIT.get(), message, predicate);
    }

    @Override
    public void run() {
        new ArrayList<>(waiters.values()).forEach((list) -> list.forEach(Runnable::run));
    }

    @Override
    public <T> ListenableFuture<T> waitForState(T obj, final String... desiredStates) {
        Set<String> desiredStatesSet = CollectionUtils.set(desiredStates);
        return waitFor(obj, "state " + desiredStatesSet, (testObject) -> {
            return desiredStatesSet.contains(ObjectUtils.getState(testObject));
        });
    }

    @Override
    public <T> ListenableFuture<List<T>> waitForState(Collection<T> objs, String... state) {
        List<ListenableFuture<T>> futures = new ArrayList<>();
        for (T obj : objs) {
            futures.add(waitForState(obj, state));
        }

        return futures.size() == 0 ? AsyncUtils.done(Collections.emptyList()) : AsyncUtils.afterAll(futures);
    }

    @Override
    public <T> ListenableFuture<T> waitRemoved(T obj) {
        return waitFor(obj, "removed", (testObj) -> ObjectUtils.getRemoved(testObj) == null);
    }

    @Override
    public String getName() {
        return "resource.change.monitor";
    }

    @Override
    public boolean isShouldLock() {
        return false;
    }

}
