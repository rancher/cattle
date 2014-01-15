package io.github.ibuildthecloud.dstack.engine.idempotent;

public interface IdempotentExecution<T> {

    T execute();

}
