package io.cattle.platform.engine.idempotent;

public abstract class IdempotentExecutionNoReturn implements IdempotentExecution<Boolean> {

    @Override
    public final Boolean execute() {
        executeNoResult();
        return Boolean.TRUE;
    }

    protected abstract void executeNoResult();
}
