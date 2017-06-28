package io.cattle.platform.metadata.service;

public interface MetadataModOperation<T> {

    T modify(T obj);

}
