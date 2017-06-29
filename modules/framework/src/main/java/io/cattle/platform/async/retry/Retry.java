package io.cattle.platform.async.retry;

import io.cattle.platform.util.concurrent.DelayedObject;

import java.util.concurrent.Future;

public class Retry {
    int retryCount;
    int retries;
    Long timeoutMillis;
    Runnable runnable;
    Future<?> future;
    boolean keepalive = false;

    public Retry(int retries, Long timeoutMillis, Future<?> future, Runnable runnable) {
        super();
        this.retryCount = 0;
        this.retries = retries;
        this.timeoutMillis = timeoutMillis;
        this.runnable = runnable;
        this.future = future;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getRetries() {
        return retries;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public Future<?> getFuture() {
        return future;
    }

    public int increment() {
        return ++retryCount;
    }

    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if (obj instanceof DelayedObject) {
            return ((DelayedObject) obj).getObject().equals(this);
        }

        return super.equals(obj);
    }

}
