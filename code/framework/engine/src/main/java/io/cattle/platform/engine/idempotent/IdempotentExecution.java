package io.cattle.platform.engine.idempotent;

public interface IdempotentExecution<T> {

    T execute();

}
