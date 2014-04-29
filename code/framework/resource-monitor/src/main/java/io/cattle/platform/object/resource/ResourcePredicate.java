package io.cattle.platform.object.resource;

public interface ResourcePredicate<T> {

    boolean evaluate(T obj);

}
