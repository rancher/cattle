package io.github.ibuildthecloud.dstack.engine.context;

import io.github.ibuildthecloud.dstack.engine.process.log.ParentLog;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;

import java.util.EmptyStackException;
import java.util.Stack;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class EngineContext {

    private static final ManagedThreadLocal<EngineContext> TL = new ManagedThreadLocal<EngineContext>() {
        @Override
        protected EngineContext initialValue() {
            return new EngineContext();
        }
    };

    Stack<ParentLog> currentLog = new Stack<ParentLog>();
    ProcessServer processServer;

    public Long getProcessingServerId() {
        return null;
    }

    public void pushLog(ParentLog log) {
        currentLog.push(log);
    }

    public void popLog() {
        currentLog.pop();
    }

    public ParentLog peekLog() {
        try {
            return currentLog.peek();
        } catch ( EmptyStackException e ) {
            return null;
        }
    }

    public static EngineContext getEngineContext() {
        return TL.get();
    }

    public ProcessServer getProcessServer() {
        return processServer;
    }

    public void setProcessServer(ProcessServer processServer) {
        this.processServer = processServer;
    }

    public static Long getProcessServerId() {
        EngineContext context = EngineContext.getEngineContext();
        ProcessServer server = context.getProcessServer();
        return server == null ? null : server.getId();
    }
}
