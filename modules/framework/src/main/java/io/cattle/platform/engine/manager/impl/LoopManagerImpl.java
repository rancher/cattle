package io.cattle.platform.engine.manager.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.object.ObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class LoopManagerImpl implements LoopManager {

    private static final Logger log = LoggerFactory.getLogger(LoopManagerImpl.class);

    LoopFactory factory;
    ExecutorService executor;
    ObjectManager objectManager;
    ScheduledExecutorService executorService;
    Cache<String, LoopWrapper> loops = CacheBuilder.newBuilder().build();

    public LoopManagerImpl(LoopFactory factory, ExecutorService executor, ObjectManager objectManager, ScheduledExecutorService executorService) {
        super();
        this.factory = factory;
        this.executor = executor;
        this.objectManager = objectManager;
        this.executorService = executorService;
    }

    @Override
    public ListenableFuture<?> kick(String name, String type, Long id, Object input) {
        if (id == null) {
            return AsyncUtils.done();
        }

        LoopWrapper loop = getLoop(name, type, id);
        if (loop == null) {
            return AsyncUtils.done();
        }

        return loop.kick(input);
    }

    protected LoopWrapper getLoop(String name, String type, Long id) {
        String key = String.format("%s/%s:%s", name, type, id);
        try {
            return this.loops.get(key, () -> {
                Loop inner = factory.buildLoop(name, type, id);
                if (inner == null) {
                    return null;
                }
                return  new LoopWrapper(name, inner, executor, executorService);
            });
        } catch (ExecutionException e) {
            log.error("Failed to build loop for {}", key, e);
            return null;
        }
    }

    @Override
    public ListenableFuture<?> kick(String name, Class<?> type, Long id, Object input) {
        return kick(name, objectManager.getType(type), id, input);
    }

}
