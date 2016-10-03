package io.cattle.platform.async.retry;

public interface RetryTimeoutService {

    Object submit(Retry retry);

    void completed(Object obj);

}
