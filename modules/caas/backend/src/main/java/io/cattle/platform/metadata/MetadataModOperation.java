package io.cattle.platform.metadata;

public interface MetadataModOperation<T> {

    T modify(T obj);

}
