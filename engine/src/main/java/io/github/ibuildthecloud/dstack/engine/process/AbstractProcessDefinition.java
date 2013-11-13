package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProcessDefinition implements ProcessDefinition {

    List<ProcessHandler> preProcessHandlers = new ArrayList<ProcessHandler>();
    List<ProcessHandler> processHandlers = new ArrayList<ProcessHandler>();
    List<ProcessHandler> postProcessHandlers = new ArrayList<ProcessHandler>();

    @Override
    public List<ProcessHandler> getPreProcessHandlers() {
        return preProcessHandlers;
    }

    @Override
    public List<ProcessHandler> getProcessHandlers() {
        return processHandlers;
    }

    @Override
    public List<ProcessHandler> getPostProcessHandlers() {
        return postProcessHandlers;
    }

}
