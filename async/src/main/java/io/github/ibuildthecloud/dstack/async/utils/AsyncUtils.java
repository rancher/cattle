package io.github.ibuildthecloud.dstack.async.utils;

import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.dstack.util.exception.UnreachableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncUtils {

    public static <T> T get(Future<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            ExceptionUtils.rethrowExpectedRuntime(t);
            throw new UnreachableException();
        } catch (TimeoutException e) {
            throw new io.github.ibuildthecloud.dstack.async.utils.TimeoutException(e);
        }
    }

    public static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if ( t instanceof TimeoutException ) {
                throw new io.github.ibuildthecloud.dstack.async.utils.TimeoutException(t);
            }
            ExceptionUtils.rethrowExpectedRuntime(t);
            throw new UnreachableException();
        }
    }
}