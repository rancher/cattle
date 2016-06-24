package io.cattle.platform.core.cache;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.iaas.event.IaasEvents;

import java.util.ArrayList;
import java.util.List;

import com.google.common.cache.Cache;

public class DBCacheManager implements AnnotatedEventListener {
    
    List<Cache<?, ?>> caches = new ArrayList<>();
    
    public <K, V> Cache<K, V> register(Cache<K, V> cache) {
        caches.add(cache);
        return cache;
    }
    
    @EventHandler(name=IaasEvents.CLEAR_CACHE)
    public void clearCache(Event event) {
        clear();
    }

    public void clear() {
        for (Cache<?, ?> cache : caches) {
            cache.invalidateAll();
        }
    }

}
