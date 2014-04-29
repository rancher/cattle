package io.cattle.platform.object.monitor.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import com.netflix.config.DynamicLongProperty;

public class ResourceMonitorImpl implements ResourceMonitor, AnnotatedEventListener, Task, TaskOptions {

    private static final DynamicLongProperty DEFAULT_WAIT = ArchaiusUtil.getLong("resource.monitor.default.wait.millis");

    ObjectManager objectManager;
    ConcurrentMap<String,Object> waiters = new ConcurrentHashMap<String, Object>();

    @EventHandler
    public void resourceChange(Event event) {
        String key = key(event.getResourceType(), event.getResourceId());
        Object wait = waiters.get(key);

        if ( wait != null ) {
            synchronized (wait) {
                wait.notifyAll();
            }
        }
    }

    protected String key(Object resourceType, Object resourceId) {
        return String.format("%s:%s", resourceType, resourceId);
    }

    @Override
    public <T> T waitFor(T obj, long timeout, ResourcePredicate<T> predicate) {
        if ( obj == null || predicate == null ) {
            return obj;
        }

        String type = objectManager.getType(obj);
        Object id = ObjectUtils.getId(obj);

        if ( type == null || id == null ) {
            throw new IllegalArgumentException("Type and id are required got [" + type +
                    "] [" + id + "]");
        }

        String key = key(type, id);
        long start = System.currentTimeMillis();

        while ( start + timeout > System.currentTimeMillis() ) {
            obj = objectManager.reload(obj);

            if ( predicate.evaluate(obj) ) {
                return obj;
            }

            Object wait = new Object();
            Object oldValue = waiters.putIfAbsent(key, wait);
            if ( oldValue != null ) {
                wait = oldValue;
            }

            long waitTime = timeout - (System.currentTimeMillis() - start);
            if ( waitTime <= 0 ) {
                break;
            }
            synchronized (wait) {
                try {
                    wait.wait(waitTime);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Interrupted", e);
                }
            }
        }

        throw new TimeoutException("Object [" + key + "] failed to satisify predicate [" + timeout + "] millis");
    }

    @Override
    public <T> T waitFor(T obj, ResourcePredicate<T> predicate) {
        return waitFor(obj, DEFAULT_WAIT.get(), predicate);
    }


    @Override
    public void run() {
        Map<String,Object> copy = new HashMap<String, Object>(waiters);
        for ( Map.Entry<String, Object> entry : copy.entrySet() ) {
            Object value = entry.getValue();
            waiters.remove(entry.getKey(), entry.getValue());
            synchronized (value) {
                value.notifyAll();
            }
        }
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

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
