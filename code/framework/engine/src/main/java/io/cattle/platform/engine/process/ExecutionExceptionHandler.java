package io.cattle.platform.engine.process;

import io.cattle.platform.util.exception.ExecutionException;

public interface ExecutionExceptionHandler {

    void handleException(ExecutionException e, ProcessState state, ProcessServiceContext context);

}
