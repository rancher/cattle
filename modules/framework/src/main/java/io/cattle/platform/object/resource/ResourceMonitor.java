package io.cattle.platform.object.resource;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.List;

public interface ResourceMonitor {

    <T> ListenableFuture<T> waitFor(T input, long timeout, String message, ResourcePredicate<T> predicate);

    <T> ListenableFuture<T> waitFor(T input, String message, ResourcePredicate<T> predicate);

    <T> ListenableFuture<T> waitForState(T obj, String... state);

    <T> ListenableFuture<List<T>> waitForState(Collection<T> obj, String... state);

    <T> ListenableFuture<T> waitRemoved(T obj);

}
