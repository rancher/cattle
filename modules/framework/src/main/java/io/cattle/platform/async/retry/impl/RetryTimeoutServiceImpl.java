package io.cattle.platform.async.retry.impl;

import com.google.common.util.concurrent.SettableFuture;
import io.cattle.platform.async.retry.CancelRetryException;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.utils.TimeoutException;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RetryTimeoutServiceImpl implements RetryTimeoutService {

    ExecutorService executorService;
    ScheduledExecutorService scheduledExecutorService;

    public RetryTimeoutServiceImpl(ExecutorService executorService, ScheduledExecutorService scheduledExecutorService) {
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public Object submit(Retry retry) {
        return queue(retry);
    }

    public void retry(Retry retry) {
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
                try {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            Object cancel = queue(retry);
                            final Runnable run = retry.getRunnable();
                            if (run != null) {
                                new NoExceptionRunnable() {
                                    @Override
                                    protected void doRun() throws Exception {
                                        try {
                                            run.run();
                                        } catch (CancelRetryException e) {
                                            completed(cancel);
                                        }
                                    }
                                }.run();
                            }
                        }
                    });
                } catch (RejectedExecutionException e) {
                    queue(retry);
                }
            }
        }
    }

    protected Object queue(Retry retry) {
        return scheduledExecutorService.schedule(() -> retry(retry), retry.getTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void completed(Object obj) {
        ((ScheduledFuture) obj).cancel(false);
    }

}
