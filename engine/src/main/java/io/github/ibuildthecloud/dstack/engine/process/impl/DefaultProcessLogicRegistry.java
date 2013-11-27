package io.github.ibuildthecloud.dstack.engine.process.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessLogicRegistry;

public class DefaultProcessLogicRegistry implements ProcessLogicRegistry {

    Map<String,ProcessHandler> handlers = new HashMap<String,ProcessHandler>();
    Map<String,ProcessListener> listeners = new HashMap<String,ProcessListener>();

    @Override
    public ProcessListener getProcessListener(String name) {
        return listeners.get(name);
    }

    @Override
    public ProcessHandler getProcessHandler(String name) {
        return handlers.get(name);
    }

    public Map<String, ProcessHandler> getHandlers() {
        return handlers;
    }

    @Inject
    public void setHandlers(Map<String, ProcessHandler> handlers) {
        this.handlers = handlers;
    }

    public Map<String, ProcessListener> getListeners() {
        return listeners;
    }

//    @Inject
    public void setListeners(Map<String, ProcessListener> listeners) {
        this.listeners = listeners;
    }

}
