package io.github.ibuildthecloud.dstack.async.retry;

import java.util.concurrent.Future;

public interface RetryTimeoutService {

    Object timeout(Future<?> future, long timeout);

    Object submit(Retry retry);

    void completed(Object obj);

}
