package io.github.ibuildthecloud.dstack.engine.context;

import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;

import java.util.Stack;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class EngineContext {

    private static final ManagedThreadLocal<EngineContext> TL = new ManagedThreadLocal<EngineContext>() {
        @Override
        protected EngineContext initialValue() {
            return new EngineContext();
        }
    };

    Stack<ProcessLog> currentProcess = new Stack<ProcessLog>();

    public Long getProcessingServerId() {
        return null;
    }

    public void pushProcessLog(ProcessLog log) {
        currentProcess.push(log);
    }

    public void popProcessLog() {
        currentProcess.pop();
    }

    public ProcessLog peekProcessLog() {
        return currentProcess.peek();
    }

    public static EngineContext getEngineContext() {
        return TL.get();
    }
}
