package io.cattle.platform.engine.context;

import static io.cattle.platform.engine.process.util.ProcessMDC.*;
import io.cattle.platform.engine.process.log.ParentLog;
import io.cattle.platform.engine.process.log.ProcessExecutionLog;
import io.cattle.platform.engine.process.log.ProcessLogicExecutionLog;
import io.cattle.platform.server.context.ServerContext;

import java.util.EmptyStackException;
import java.util.Stack;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;

public class EngineContext {

    private static final ManagedThreadLocal<EngineContext> TL = new ManagedThreadLocal<EngineContext>() {
        @Override
        protected EngineContext initialValue() {
            return new EngineContext();
        }
    };

    Stack<ParentLog> currentLog = new Stack<ParentLog>();

    public void pushLog(ParentLog log) {
        currentLog.push(log);
        setupMdc();
    }

    public void popLog() {
        currentLog.pop();
        setupMdc();
    }

    protected void setupMdc() {
        Long processId = null;
        String logicName = null, processUuid = null, processName = null, topProcessName = null, resourceId = null;
        String resourceType = null, topResourceId = null, topResourceType = null;

        StringBuilder fullPath = new StringBuilder();

        for (ParentLog log : currentLog) {
            if (fullPath.length() > 0) {
                fullPath.append("->");
            }

            if (log instanceof ProcessExecutionLog) {
                fullPath.append(log.getName());

                if (processUuid == null) {
                    processUuid = ((ProcessExecutionLog) log).getId();
                }
                if (processId == null) {
                    processId = ((ProcessExecutionLog) log).getProcessId();
                }
                if (topProcessName == null) {
                    topProcessName = log.getName();
                }
                if (topResourceType == null) {
                    topResourceType = ((ProcessExecutionLog) log).getResourceType();
                }
                if (topResourceId == null) {
                    topResourceId = ((ProcessExecutionLog) log).getResourceId();
                }

                processName = log.getName();
                resourceType = ((ProcessExecutionLog) log).getResourceType();
                resourceId = ((ProcessExecutionLog) log).getResourceId();
            } else if (log instanceof ProcessLogicExecutionLog) {
                fullPath.append("(").append(log.getName()).append(")");
                logicName = log.getName();
            }
        }

        String prettyResource = null;
        String prettyProcess = null;
        StringBuilder buffer = new StringBuilder();
        if (topResourceType != null) {
            buffer.append(topResourceType).append(":").append(topResourceId);
            if (!ObjectUtils.equals(resourceId, topResourceId) || !ObjectUtils.equals(resourceType, topResourceType)) {
                buffer.append("->").append(resourceType).append(":").append(resourceId);
            }
            prettyResource = buffer.toString();
        }

        buffer.setLength(0);
        if (topProcessName != null) {
            buffer.append(topProcessName);
            if (!ObjectUtils.equals(topProcessName, processName)) {
                buffer.append("->").append(processName);
            }
            prettyProcess = buffer.toString();
        }

        MDC.put(PROCESS_ID, processId == null ? null : processId.toString());
        MDC.put(LOGIC_NAME, logicName);
        MDC.put(PROCESS_UUID, processUuid);
        MDC.put(PROCESS_NAME, processName);
        MDC.put(TOP_PROCESS_NAME, topProcessName);
        MDC.put(RESOURCE_ID, resourceId);
        MDC.put(RESOURCE_TYPE, resourceType);
        MDC.put(TOP_RESOURCE_ID, topResourceId);
        MDC.put(TOP_RESOURCE_TYPE, topResourceType);
        MDC.put(PRETTY_PROCESS, prettyProcess);
        MDC.put(PRETTY_RESOURCE, prettyResource);
        MDC.put(LOGIC_PATH, fullPath.toString());
    }

    public ParentLog peekLog() {
        try {
            return currentLog.peek();
        } catch (EmptyStackException e) {
            return null;
        }
    }

    public static boolean hasParentProcess() {
        return EngineContext.getEngineContext().peekLog() != null;
    }

    public static boolean isNestedExecution() {
        EngineContext context = EngineContext.getEngineContext();
        return context != null && context.currentLog.size() > 1;
    }

    public static EngineContext getEngineContext() {
        return TL.get();
    }

    public static String getProcessServerId() {
        return ServerContext.getServerId();
    }
}
