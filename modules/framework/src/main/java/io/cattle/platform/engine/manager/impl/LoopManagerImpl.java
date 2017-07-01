package io.cattle.platform.engine.manager.impl;

import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ListenableFuture;

public class LoopManagerImpl implements LoopManager {

    Map<String, Map<String, LoopWrapper>> loops = new HashMap<>();
    LoopFactory factory;
    ExecutorService executor;
    ObjectManager objectManager;
    ScheduledExecutorService executorService;

    public LoopManagerImpl(LoopFactory factory, ExecutorService executor, ObjectManager objectManager, ScheduledExecutorService executorService) {
        super();
        this.factory = factory;
        this.executor = executor;
        this.objectManager = objectManager;
        this.executorService = executorService;
    }

    @Override
    public synchronized ListenableFuture<?> kick(String name, String type, Long id, Object input) {
        if (id == null) {
            return AsyncUtils.done();
        }
        String resourceKey = String.format("%s:%s", type, id);
        LoopWrapper loop = getLoop(name, resourceKey);
        if (loop == null) {
            loop = buildLoop(name, resourceKey, type, id);
        }
        if (loop == null) {
            return AsyncUtils.done();
        }

        return loop.kick(input);
    }

    protected LoopWrapper buildLoop(String name, String resourceKey, String type, Long id) {
        Loop inner = factory.buildLoop(name, type, id);
        LoopWrapper loop = new LoopWrapper(name, inner, executor, executorService);
        Map<String, LoopWrapper> loops = this.loops.get(name);
        if (loops == null) {
            loops = new HashMap<>();
            this.loops.put(name, loops);
        }
        loops.put(resourceKey, loop);
        return loop;
    }

    protected LoopWrapper getLoop(String name, String resourceKey) {
        Map<String, LoopWrapper> loops = this.loops.get(name);
        if (loops == null) {
            return null;
        }
        return loops.get(resourceKey);
    }

    @Override
    public ListenableFuture<?> kick(String name, Class<?> type, Long id, Object input) {
        return kick(name, objectManager.getType(type), id, input);
    }

}
