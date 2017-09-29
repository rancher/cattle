package io.cattle.platform.condition.deployment;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoveBackoff {

    ExecutorService executorService;
    LoadingCache<String, Value> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .removalListener(this::onRemoval)
            .build(new CacheLoader<String, Value>() {
                @Override
                public Value load(String key) throws Exception {
                    return new Value();
                }
            });

    public RemoveBackoff(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public boolean check(long deploymentUnitId, String lcName, Runnable callback) {
        String key = deploymentUnitId + "/" + lcName;
        Value value = cache.getUnchecked(key);
        if (value.count > 3) {
            value.callbacks.add(callback);
            return false;
        }

        return true;
    }

    protected void onRemoval(RemovalNotification<String, Value> n) {
        n.getValue().callbacks.forEach(executorService::submit);
    }

    public static class Value {
        int count;
        List<Runnable> callbacks = Collections.synchronizedList(new ArrayList<>());
    }
}
