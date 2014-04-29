package io.cattle.platform.object.resource;

public interface ResourceMonitor {

    <T> T waitFor(T obj, long timeout, ResourcePredicate<T> predicate);

    <T> T waitFor(T obj, ResourcePredicate<T> predicate);

}
