package io.cattle.platform.lock.impl;

import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.BlockingLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.MultiLockDefinition;
import io.cattle.platform.lock.provider.LockProvider;
import io.cattle.platform.util.type.InitializationTask;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;

public class LockDelegatorImpl implements LockDelegator, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(LockDelegatorImpl.class);

    LockManager lockManager;
    Map<String, Lock> holding = new ConcurrentHashMap<String, Lock>();
    BlockingQueue<LockOp> ops = new LinkedBlockingQueue<LockOp>();
    ExecutorService executorService;
    boolean shutdown = false;

    @Override
    public boolean isLocked(LockDefinition lockDef) {
        if (!acceptableLock(lockDef)) {
            return false;
        }

        return holding.containsKey(lockDef.getLockId());
    }

    @Override
    public boolean tryLock(LockDefinition lockDef) {
        return doOp(lockDef, true);
    }

    @Override
    public boolean unlock(LockDefinition lockDef) {
        return doOp(lockDef, false);
    }

    protected boolean doOp(LockDefinition lockDef, boolean lock) {
        SettableFuture<Boolean> future = SettableFuture.create();
        ops.add(new LockOp(lockDef, lock, future));

        return AsyncUtils.get(future);
    }

    @Override
    public void start() {
        executorService.execute(new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                runLoop();
            }
        });
    }

    public void stop() {
        shutdown = true;
    }

    protected void runLoop() {
        /* This loop should never end, unless it has been shutdown */
        LockOp op = null;
        while (true) {
            try {
                op = ops.take();
                try {
                    if (op.lock) {
                        lock(op);
                    } else {
                        unlock(op);
                    }
                } catch (Throwable t) {
                    op.future.set(false);
                    log.error("Exception in lock delegator, lockdef [{}]", op.lock, t);
                }
            } catch (Throwable t) {
                if (shutdown) {
                    return;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.info("Interrupted", e);
                }
            }

            if (shutdown) {
                return;
            }
        }
    }

    protected boolean acceptableLock(LockDefinition lockDef) {
        if (lockDef instanceof MultiLockDefinition) {
            log.error("Can not lock a multilock with a lock delegator");
            return false;
        }

        if (lockDef instanceof BlockingLockDefinition) {
            log.error("Can not lock a blocking lock with a lock delegator");
            return false;
        }

        return true;
    }

    protected void lock(LockOp op) {
        if (!acceptableLock(op.def)) {
            op.future.set(false);
            return;
        }

        if (holding.containsKey(op.def.getLockId())) {
            op.future.set(true);
            return;
        }

        LockProvider lockProvider = lockManager.getLockProvider();
        boolean success = false;
        Lock lock = lockProvider.getLock(op.def);
        try {
            success = lock.tryLock();
            if (success) {
                log.trace("Acquired lock [{}]", op.def.getLockId());
            }
        } finally {
            if (success) {
                holding.put(op.def.getLockId(), lock);
                op.future.set(true);
            } else {
                op.future.set(false);
                lockProvider.releaseLock(lock);
            }
        }
    }

    protected void unlock(LockOp op) {
        Lock lock = holding.get(op.def.getLockId());
        if (lock != null) {
            LockProvider lockProvider = lockManager.getLockProvider();
            lock.unlock();
            log.trace("Released lock [{}]", op.def.getLockId());
            holding.remove(op.def.getLockId());
            lockProvider.releaseLock(lock);
        }
        op.future.set(true);
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private static final class LockOp {
        LockDefinition def;
        boolean lock;
        SettableFuture<Boolean> future;

        public LockOp(LockDefinition def, boolean lock, SettableFuture<Boolean> future) {
            super();
            this.def = def;
            this.lock = lock;
            this.future = future;
        }
    }

}
