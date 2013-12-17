package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;
import io.github.ibuildthecloud.dstack.extension.ExtensionManager;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class AbstractProcessDefinition implements ProcessDefinition {

    public static final String PRE = "process.%s.pre.listeners";
    public static final String HANDLER = "process.%s.handlers";
    public static final String POST = "process.%s.post.listeners";

    String preProcessListenersKey;
    String processHandlersKey;
    String postProcessListenersKey;
    ExtensionManager extensionManager;
    String name;

    @PostConstruct
    public void init() {
        preProcessListenersKey = String.format(PRE, getName());
        processHandlersKey = String.format(HANDLER, getName());
        postProcessListenersKey = String.format(POST, getName());
    }


    @Override
    public List<ProcessHandler> getProcessHandlers() {
        return extensionManager.getExtensionList(processHandlersKey, ProcessHandler.class);
    }

    protected List<ProcessListener> getListeners(String key) {
        return extensionManager.getExtensionList(key, ProcessListener.class);
    }

    @Override
    public List<ProcessListener> getPreProcessListeners() {
        return getListeners(preProcessListenersKey);
    }

    @Override
    public List<ProcessListener> getPostProcessListeners() {
        return getListeners(postProcessListenersKey);
    }

    @Override
    public String getName() {
        return name;
    }

    @Inject
    public void setName(String name) {
        this.name = name;
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    @Inject
    public void setExtensionManager(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

}
