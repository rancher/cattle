package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessHandlerRegistry;
import io.cattle.platform.engine.process.ProcessRouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProcessHandlerRegistryImpl implements ProcessHandlerRegistry, ProcessRouter {

    Map<String, ProcessDefinition> processDefinitions;
    Map<String, List<ProcessHandler>> handlers = new HashMap<>();

    public ProcessHandlerRegistryImpl(Map<String, ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

    @Override
    public List<ProcessHandler> getHandlers(String processName) {
        List<ProcessHandler> result = handlers.get(processName);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public ProcessRouter handle(String process, ProcessHandler... handlers) {
        if (process.contains("*")) {
            return handleWildcard(process, handlers);
        }

        List<ProcessHandler> list = getList(process);
        for (ProcessHandler handler : handlers)  {
            list.add(handler);
        }
        return this;
    }

    @Override
    public ProcessRouter preHandle(String process, ProcessHandler... handlers) {
        if (process.contains("*")) {
            return preHandleWildcard(process, handlers);
        }
        List<ProcessHandler> list = getList(process);
        for (int i = handlers.length; i > 0 ; i--) {
            list.add(0, handlers[i-1]);
        }
        return this;
    }

    private List<ProcessHandler> getList(String process) {
        List<ProcessHandler> processes = handlers.get(process);
        if (processes == null) {
            processes = new ArrayList<>();
            handlers.put(process, processes);
        }
        return processes;
    }

    private ProcessRouter handleWildcard(String process, ProcessHandler... handlers) {
        String[] parts = process.split("[*]");
        for (int i = 0 ; i < parts.length ; i++) {
            parts[i] = Pattern.quote(parts[i]);
        }
        Pattern p = Pattern.compile(String.join(".*", parts) + (process.endsWith("*") ? ".*" : ""));
        processDefinitions.keySet().stream().forEach((name) -> {
            if (p.matcher(name).matches()) {
                handle(name, handlers);
            }
        });
        return this;
    }

    private ProcessRouter preHandleWildcard(String process, ProcessHandler... handlers) {
        Pattern p = Pattern.compile(process.replace("*", ".*"));
        processDefinitions.keySet().stream().forEach((name) -> {
            if (p.matcher(name).matches()) {
                preHandle(name, handlers);
            }
        });
        return this;
    }

    @Override
    public void addProcess(ProcessDefinition processDefinition) {
        processDefinitions.put(processDefinition.getName(), processDefinition);
    }

}