package io.cattle.platform.async.retry.impl;

import io.cattle.platform.async.retry.CancelRetryException;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.util.concurrent.DelayedObject;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.google.common.util.concurrent.SettableFuture;

public class RetryTimeoutServiceImpl implements RetryTimeoutService {

    DelayQueue<DelayedObject<Retry>> retryQueue = new DelayQueue<DelayedObject<Retry>>();
    ExecutorService executorService;

    @Override
    public Object submit(Retry retry) {
        return queue(retry);
    }

    public void retry() {
        DelayedObject<Retry> delayed = retryQueue.poll();
        while (delayed != null) {
            final Retry retry = delayed.getObject();

            if (retry.isKeepalive()) {
                retry.setKeepalive(false);
                queue(retry);
            } else {
                retry.increment();

                if (retry.getRetryCount() >= retry.getRetries()) {
                    Future<?> future = retry.getFuture();
                    if (future instanceof SettableFuture) {
                        ((SettableFuture<?>) future).setException(new TimeoutException());
                    } else {
                        future.cancel(true);
                    }
                } else {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                                queue(retry);
                                final Runnable run = retry.getRunnable();
                                if (run != null) {
                                    new NoExceptionRunnable() {
                                        @Override
                                        protected void doRun() throws Exception {
                                            try {
                                                run.run();
                                            } catch (CancelRetryException e) {
                                                completed(retry);
                                            }
                                        }
                                    }.run();
                                }
                        }
                    });
                }
            }

            delayed = retryQueue.poll();
        }
    }

    protected DelayedObject<Retry> queue(Retry retry) {
        DelayedObject<Retry> delayed = new DelayedObject<Retry>(System.currentTimeMillis() + retry.getTimeoutMillis(), retry);
        retryQueue.add(delayed);
        return delayed;
    }

    @Override
    public void completed(Object obj) {
        retryQueue.remove(obj);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

}
