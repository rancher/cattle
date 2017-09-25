package io.cattle.platform.condition;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import org.apache.cloudstack.managed.context.NoException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Condition<T> {

    private final Object conditionLock = new Object();

    ExecutorService executorService;
    ConditionValues<T> conditionValues;
    Cache<String, Boolean> conditions;
    ListValuedMap<String, Runnable> callbacks = new ArrayListValuedHashMap<>();

    public Condition(ExecutorService executorService, ConditionValues<T> conditionValues) {
        this.executorService = executorService;
        this.conditionValues = conditionValues;
        this.conditions = CacheBuilder.newBuilder()
                .expireAfterAccess(2, TimeUnit.HOURS)
                .removalListener(this::onRemoval)
                .build();
        conditionValues.setValueSetters(this::setValue);
    }

    public boolean check(T obj, Runnable callback) {
        String key = conditionValues.getKey(obj);
        Boolean value;
        synchronized (conditionLock) {
            try {
                value = conditions.get(key, () -> conditionValues.loadInitialValue(obj));
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
            if (value != null && value) {
                return true;
            }
            if (callback != null) {
                callbacks.put(key, callback);
            }
        }

        return false;
    }

    public void onRemoval(RemovalNotification<String, Boolean> notification) {
        List<Runnable> toTrigger;
        synchronized (conditionLock) {
            toTrigger = callbacks.remove(notification.getKey());
        }
        if (toTrigger != null) {
            for (Runnable run : toTrigger) {
                executorService.submit((NoException) run::run);
            }
        }
    }

    private void setValue(String key, Boolean value) {
        if (value == null) {
            value = false;
        }
        List<Runnable> toTrigger = Collections.emptyList();
        synchronized (conditionLock) {
            conditions.put(key, value);
            if (value) {
                toTrigger = callbacks.remove(key);
            }
        }
        if (toTrigger != null) {
            for (Runnable run : toTrigger) {
                executorService.submit((NoException) run::run);
            }
        }
    }

}
