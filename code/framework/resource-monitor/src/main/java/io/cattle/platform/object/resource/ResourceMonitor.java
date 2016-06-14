package io.cattle.platform.object.resource;

public interface ResourceMonitor {
    public static final String ERROR_MSG = "failed to satisfy predicate";

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
}
