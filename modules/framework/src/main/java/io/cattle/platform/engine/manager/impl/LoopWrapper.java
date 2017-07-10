package io.cattle.platform.engine.manager.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.engine.model.Loop.Result;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LoopWrapper {

    enum State { SLEEPING, BACKOFF, PROCESS_WAIT, ERROR, RUNNING }

    private static final DynamicLongProperty DEFAULT_TOKEN_INTERVAL = ArchaiusUtil.getLong("loop.default.execution.token.every.millis");
    private static final DynamicLongProperty DEFAULT_TOKENS_MAX = ArchaiusUtil.getLong("loop.default.execution.tokens.max");
    private static final DynamicLongProperty LOOP_PROCESS_WAIT = ArchaiusUtil.getLong("loop.process.wait.delay.millis");
    private static final Logger log = LoggerFactory.getLogger(LoopWrapper.class);

    ExecutorService executor;
    ScheduledExecutorService scheduledExecutor;

    long lastRun = 0;
    long tokens = 0;

    Set<Long> waitProcesses = Collections.synchronizedSet(new HashSet<>());
    String name;
    Loop inner;
    int requested = 1, applied;
    State state = State.SLEEPING;
    DynamicLongProperty tokenInterval = ArchaiusUtil.getLong("execution.token.every.millis");
    DynamicLongProperty tokensMax = ArchaiusUtil.getLong("execution.tokens.max");
    List<Waiter> waiters = new ArrayList<>();

    public LoopWrapper(String name, Loop inner, ExecutorService executor, ScheduledExecutorService scheduledExecutor) {
        super();
        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.name = name;
        this.inner = inner;
        this.tokenInterval = ArchaiusUtil.getLong("loop." + name + ".execution.token.every.millis");
        this.tokensMax = ArchaiusUtil.getLong("loop." + name + ".execution.tokens.max");
    }

    public synchronized ListenableFuture<?> kick(Object input) {
        Waiter w = new Waiter(++requested);
        waiters.add(w);
        if (state == State.SLEEPING) {
            run(false);
        }
        return w.future;
    }

    public synchronized void processDone(Long id) {
        if (waitProcesses.remove(id) && waitProcesses.size() == 0) {
            kick(null);
        }
    }

    protected void setState(State state) {
        this.state = state;
    }

    protected synchronized void run(boolean ignoreProcesses) {
        if (!ignoreProcesses && waitProcesses.size() > 0) {
            setState(State.PROCESS_WAIT);
            runAfter(true, LOOP_PROCESS_WAIT.get());
            return;
        }

        if (requested == applied) {
            setState(State.SLEEPING);
            return;
        }

        long delay = getRunDelay();
        if (delay > 0) {
            setState(State.BACKOFF);
            runAfter(false, delay);
            return;
        }

        setState(State.RUNNING);
        executor.execute(new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                int startValue = requested;
                long start = System.currentTimeMillis();
                Loop.Result result = null;
                try {
                    result = inner.run(null);
                } finally {
                    log.info("Loop [{}] [{}] {}/{}/{} {}ms", name, result, requested, startValue, applied,
                            (System.currentTimeMillis()-start));
                    synchronized (LoopWrapper.this) {
                        if (result == null) {
                            setState(State.ERROR);
                            runAfter(false, 2000L);
                        } else {
                            applied = startValue;
                            if (result == Result.DONE) {
                                waiters = waiters.stream().filter((w) -> {
                                    if (w.requested <= applied) {
                                        w.future.set(applied);
                                        return false;
                                    }
                                    return true;
                                }).collect(Collectors.toList());
                            }
                            runAfter(false, 0L);
                        }
                    }
                }
            }
        });
    }

    private void runAfter(boolean ignoreProcesses, long delay) {
        scheduledExecutor.schedule(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                LoopWrapper.this.run(ignoreProcesses);
            }

        }, delay, TimeUnit.MILLISECONDS);
    }

    protected long getRunDelay() {
        long interval = tokenInterval.get();
        if (interval <= 0) {
            interval = DEFAULT_TOKEN_INTERVAL.get();
        }

        long max = tokensMax.get();
        if (max <= 0) {
            max = DEFAULT_TOKENS_MAX.get();
        }

        long now = System.currentTimeMillis();
        tokens += (now - lastRun)/interval;

        if (tokens > max) {
            tokens = max;
        }

        lastRun = now;

        if (tokens == 0) {
            return interval;
        }

        tokens--;
        return 0;
    }

    private static class Waiter {
        int requested;
        SettableFuture<Object> future = SettableFuture.create();

        public Waiter(int requested) {
            super();
            this.requested = requested;
        }
    }
}
