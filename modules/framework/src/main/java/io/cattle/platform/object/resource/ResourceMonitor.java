package io.cattle.platform.object.resource;

public interface ResourceMonitor {
    /**
     * 
     * @param obj
     * @param timeout (milliseconds)
     * @param predicate
     * @return
     */
    <T> T waitFor(T obj, long timeout, ResourcePredicate<T> predicate);

    <T> T waitFor(T obj, ResourcePredicate<T> predicate);

    <T> T waitForState(T obj, String state);

    <T> T waitForNotTransitioning(T obj);

    <T> T waitForNotTransitioning(T obj, long timeout);
}
