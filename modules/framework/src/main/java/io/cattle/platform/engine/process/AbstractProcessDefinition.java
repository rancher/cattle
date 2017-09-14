package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;

import java.util.List;

public abstract class AbstractProcessDefinition implements ProcessDefinition {

    ProcessHandlerRegistry registry;
    String name;

    public AbstractProcessDefinition(String name, ProcessHandlerRegistry registry) {
        this.name = name;
        this.registry = registry;
    }

    @Override
    public List<ProcessHandler> getProcessHandlers() {
        return registry.getHandlers(name);
    }

    @Override
    public String getName() {
        return name;
    }

}
