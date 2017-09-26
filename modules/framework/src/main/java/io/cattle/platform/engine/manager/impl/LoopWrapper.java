package io.cattle.platform.engine.manager.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.engine.model.Loop.Result;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import org.apache.cloudstack.managed.context.InContext;
import org.apache.cloudstack.managed.context.NoException;
import org.jooq.exception.DataChangedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class LoopWrapper {

    enum State { SLEEPING, BACKOFF, ERROR, RUNNING }

    private static final DynamicLongProperty DEFAULT_TOKEN_INTERVAL = ArchaiusUtil.getLong("loop.default.execution.token.every.millis");
    private static final DynamicLongProperty DEFAULT_TOKENS_MAX = ArchaiusUtil.getLong("loop.default.execution.tokens.max");
    private static final Logger log = LoggerFactory.getLogger(LoopWrapper.class);

    ExecutorService executor;
    ScheduledExecutorService scheduledExecutor;

    long lastRun = 0;
    long tokens = 0;

    String name;
    Loop inner;
    Object collectionLock = new Object();
    int requested = 1, applied;
    State state = State.SLEEPING;
    DynamicLongProperty tokenInterval = ArchaiusUtil.getLong("execution.token.every.millis");
    DynamicLongProperty tokensMax = ArchaiusUtil.getLong("execution.tokens.max");
    List<Waiter> waiters = new ArrayList<>();
    List<Object> inputs = new ArrayList<>();
    Throwable lastError;

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

        synchronized (collectionLock) {
            waiters.add(w);
            scheduledExecutor.schedule(() -> w.future.setException(new TimeoutException()), 2, TimeUnit.MINUTES);

            if (input instanceof List<?>) {
                inputs.addAll((List<?>) input);
            } else {
                inputs.add(input);
            }
        }

        if (state == State.SLEEPING) {
            scheduleRun();
        }
        return w.future;
    }

    private synchronized void setState(State state) {
        this.state = state;
    }

    private synchronized void scheduleRun() {
        if (requested == applied) {
            setState(State.SLEEPING);
            return;
        }

        long delay = getRunDelay();
        if (delay > 0) {
            setState(State.BACKOFF);
            runAfter(delay);
            return;
        }

        setState(State.RUNNING);
        executor.execute((NoException) this::executeLoop);
    }

    private void executeLoop() {
        int startValue = requested;
        long start = System.currentTimeMillis();
        List<Object> inputs;
        Loop.Result result = null;
        long delay = 0L;
        try {
            synchronized (collectionLock) {
                inputs = this.inputs;
                this.inputs = new ArrayList<>();
            }
            result = DeferredUtils.nest(() -> inner.run(inputs));
        } catch (ProcessDelayException e) {
            delay = e.getRunAfter().getTime() - System.currentTimeMillis();
            if (delay > 0) {
                result = Result.WAITING;
            } else {
                delay = 0L;
            }
        } catch (DataChangedException ignored) {
        } catch (Throwable t) {
            lastError = t;
        } finally {
            if (result == null) {
                // Loops should never return null, so a null result means we got here due to an exception being thrown
                log.error("Loop [{}] [{}] {}/{}/{} {}ms", name, result, requested, startValue, applied,
                        (System.currentTimeMillis()-start), lastError);
                setState(State.ERROR);
                delay = 2000L;
            } else {
                applied = startValue;
                if (result == Result.DONE) {
                    // Waiters are not notified if the loop returns WAITING
                    notifyWaiters();
                }
            }

            // Continue loop
            runAfter(delay);
            log.info("Loop [{}] [{}] {}/{}/{} {}ms", name, result, requested, startValue, applied,
                    (System.currentTimeMillis()-start));
        }
    }

    private void notifyWaiters() {
        List<Waiter> toNotify = new ArrayList<>();

        synchronized (collectionLock) {
            waiters = waiters.stream().filter((w) -> {
                if (w.requested <= applied) {
                    toNotify.add(w);
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        }

        // Notify waiters w/o hold in the lock.  This is because some ListenableFutures do bad things and run long
        for (Waiter w : toNotify) {
            try {
                w.future.set(applied);
            } catch (Throwable t) {
                log.error("Failed notifying waiter in loop [{}]", name, t);
            }
        }
    }

    private void runAfter(long delay) {
        scheduledExecutor.schedule((InContext) LoopWrapper.this::scheduleRun, delay, TimeUnit.MILLISECONDS);
    }

    private long getRunDelay() {
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
