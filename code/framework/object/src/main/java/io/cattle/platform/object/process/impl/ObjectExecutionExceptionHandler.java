package io.cattle.platform.object.process.impl;

import java.util.Map;

import io.cattle.platform.engine.process.ExecutionExceptionHandler;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.util.exception.ExecutionException;

public class ObjectExecutionExceptionHandler implements ExecutionExceptionHandler {

    @Override
    public void handleException(ExecutionException e, ProcessState state, ProcessServiceContext context) {
        if ( state.getResource() == e.getResource() ) {
            Map<String,Object> data = TransitioningUtils.getTransitioningData(e);
            state.applyData(data);
        }
    }

}
