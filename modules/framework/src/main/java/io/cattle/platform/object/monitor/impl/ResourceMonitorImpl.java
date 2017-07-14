package io.cattle.platform.object.monitor.impl;

import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.ResourceTimeoutException;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskOptions;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResourceMonitorImpl implements ResourceMonitor, AnnotatedEventListener, Task, TaskOptions {

    private static final DynamicLongProperty DEFAULT_WAIT = ArchaiusUtil.getLong("resource.monitor.default.wait.millis");

    ObjectManager objectManager;
    ConcurrentMap<String, Object> waiters = new ConcurrentHashMap<>();
    ObjectMetaDataManager objectMetaDataManger;
    IdFormatter idFormatter;
    Set<String> seen = new HashSet<>();

    public ResourceMonitorImpl(ObjectManager objectManager, ObjectMetaDataManager objectMetaDataManger, IdFormatter idFormatter) {
        super();
        this.objectManager = objectManager;
        this.objectMetaDataManger = objectMetaDataManger;
        this.idFormatter = idFormatter;
    }

    @EventHandler
    public void stateChanged(Event event) {
        resourceChange(event);
    }

    @EventHandler
    public void resourceChange(Event event) {
        String key = key(event.getResourceType(), event.getResourceId());
        Object wait = waiters.get(key);

        if (wait != null) {
            synchronized (wait) {
                wait.notifyAll();
            }
        }
    }

    protected String key(Object resourceType, Object resourceId) {
        resourceId = idFormatter.formatId(resourceType.toString(), resourceId);
        return String.format("%s:%s", resourceType, resourceId);
    }

    @Override
    public <T> T waitFor(T obj, long timeout, ResourcePredicate<T> predicate) {
        if (obj == null || predicate == null) {
            return obj;
        }

        String type = objectManager.getType(obj);
        String kind = ObjectUtils.getKind(obj);
        if (kind == null) {
            kind = type;
        }
        Object id = ObjectUtils.getId(obj);

        if (type == null || id == null) {
            throw new IllegalArgumentException("Type and id are required got [" + type + "] [" + id + "]");
        }

        String printKey = key(kind, id);
        String key = key(type, id);
        long end = System.currentTimeMillis() + timeout;
        Object wait = new Object();
        Object oldValue = waiters.putIfAbsent(key, wait);
        if (oldValue != null) {
            wait = oldValue;
        }

        synchronized (wait) {
            while (System.currentTimeMillis() < end) {
                obj = objectManager.reload(obj);

                if (predicate.evaluate(obj)) {
                    return obj;
                }

                synchronized (wait) {
                    try {
                        wait.wait(5000L);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Interrupted", e);
                    }
                }
            }
        }

        throw new ResourceTimeoutException(obj, "Waiting: " + predicate.getMessage() + " [" + printKey + "]");
    }

    @Override
    public <T> T waitFor(T obj, ResourcePredicate<T> predicate) {
        return waitFor(obj, DEFAULT_WAIT.get(), predicate);
    }

    @Override
    public void run() {
        Set<String> previouslySeen = this.seen;
        this.seen = new HashSet<>();
        Map<String, Object> copy = new HashMap<>(waiters);

        for (Map.Entry<String, Object> entry : copy.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            seen.add(key);

            if (previouslySeen.contains(key)) {
                waiters.remove(entry.getKey(), entry.getValue());
                synchronized (value) {
                    value.notifyAll();
                }
            }
        }
    }

    @Override
    public <T> T waitForState(T obj, final String desiredState) {
        return waitFor(obj, new ResourcePredicate<T>() {
            @Override
            public boolean evaluate(T obj) {
                return desiredState.equals(ObjectUtils.getState(obj));
            }

            @Override
            public String getMessage() {
                return "state to equal " + desiredState;
            }
        });
    }

    @Override
    public String getName() {
        return "resource.change.monitor";
    }

    @Override
    public boolean isShouldRecord() {
        return false;
    }

    @Override
    public boolean isShouldLock() {
        return false;
    }

}
