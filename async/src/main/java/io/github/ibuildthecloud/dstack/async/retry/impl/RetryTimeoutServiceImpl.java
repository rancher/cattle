package io.github.ibuildthecloud.dstack.async.retry.impl;

import io.github.ibuildthecloud.dstack.async.retry.Retry;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.util.concurrent.DelayedObject;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.google.common.util.concurrent.SettableFuture;

public class RetryTimeoutServiceImpl implements RetryTimeoutService {

    DelayQueue<DelayedObject<Retry>> retryQueue = new DelayQueue<DelayedObject<Retry>>();

    @Override
    public Object timeout(Future<?> future, long timeout) {
        return submit(new Retry(0, timeout, future, null));
    }

    @Override
    public Object submit(Retry retry) {
        return queue(retry);
    }

    public void retry() {
        DelayedObject<Retry> delayed = retryQueue.poll();
        while ( delayed != null ) {
            Retry retry = delayed.getObject();
            retry.increment();

            if ( retry.getRetryCount() >= retry.getRetries() ) {
                Future<?> future = retry.getFuture();
                if ( future instanceof SettableFuture ) {
                    ((SettableFuture<?>)future).setException(new TimeoutException());
                } else {
                    future.cancel(true);
                }
            } else {
                queue(retry);
                final Runnable run = retry.getRunnable();
                if ( run != null ) {
                    new NoExceptionRunnable() {
                        @Override
                        protected void doRun() throws Exception {
                            run.run();
                        }
                    }.run();
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

}
