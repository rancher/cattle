package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessHandlerRegistry;
import io.cattle.platform.engine.process.ProcessRouter;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProcessHandlerRegistryImpl implements ProcessHandlerRegistry, ProcessRouter {

    Map<String, ProcessDefinition> processDefinitions;
    ListValuedMap<String, ProcessHandler> handlers = new ArrayListValuedHashMap<>();

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

        for (ProcessHandler handler : handlers) {
            this.handlers.put(process, handler);
        }

        return this;
    }

    private ProcessRouter handleWildcard(String process, ProcessHandler... handlers) {
        String[] parts = process.split("[*]");
        for (int i = 0 ; i < parts.length ; i++) {
            parts[i] = Pattern.quote(parts[i]);
        }
        Pattern p = Pattern.compile(String.join(".*", parts) + (process.endsWith("*") ? ".*" : ""));
        processDefinitions.keySet().forEach((name) -> {
            if (p.matcher(name).matches()) {
                handle(name, handlers);
            }
        });
        return this;
    }

    @Override
    public void addProcess(ProcessDefinition processDefinition) {
        processDefinitions.put(processDefinition.getName(), processDefinition);
    }

}