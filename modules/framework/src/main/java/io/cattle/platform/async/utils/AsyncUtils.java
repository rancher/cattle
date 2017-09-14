package io.cattle.platform.async.utils;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.UnreachableException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

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
            throw new io.cattle.platform.async.utils.TimeoutException(e);
        }
    }

    public static <T> T get(Future<T> future) {
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof TimeoutException) {
                throw new io.cattle.platform.async.utils.TimeoutException(t);
            }
            ExceptionUtils.rethrowExpectedRuntime(t);
            throw new UnreachableException();
        } catch (TimeoutException e) {
            throw new io.cattle.platform.async.utils.TimeoutException(e);
        }
    }

    public static <T> ListenableFuture<T> done() {
        return done(null);
    }

    public static <T> ListenableFuture<T> done(T obj) {
        SettableFuture<T> future = SettableFuture.create();
        future.set(obj);

        return future;
    }

    public static <T> ListenableFuture<T> error(Throwable t) {
        SettableFuture<T> future = SettableFuture.create();
        future.setException(t);

        return future;
    }

    public static <T> ListenableFuture<List<T>> afterAll(List<ListenableFuture<T>> futures) {
        return Futures.allAsList(futures);
    }

    public static <T, R> ListenableFuture<R> andThen(ListenableFuture<T> future, Function<T, R> func) {
        return Futures.transform(future, func::apply);
    }

}