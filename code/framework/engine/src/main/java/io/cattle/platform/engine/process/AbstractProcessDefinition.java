package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.extension.ExtensionManager;
import io.cattle.platform.extension.ExtensionPoint;

import java.util.List;

public abstract class AbstractProcessDefinition implements ProcessDefinition, ExtensionBasedProcessDefinition {

    public static final String PRE = "process.%s.pre.listeners";
    public static final String HANDLER = "process.%s.handlers";
    public static final String POST = "process.%s.post.listeners";

    String preProcessListenersKey;
    String processHandlersKey;
    String postProcessListenersKey;
    ExtensionManager extensionManager;
    String name;

    public AbstractProcessDefinition(String name, ExtensionManager extensionManager) {
        this.name = name;
        this.extensionManager = extensionManager;
        preProcessListenersKey = String.format(PRE, getName());
        processHandlersKey = String.format(HANDLER, getName());
        postProcessListenersKey = String.format(POST, getName());
    }

    @Override
    public List<ProcessHandler> getProcessHandlers() {
        return extensionManager.getExtensionList(processHandlersKey, ProcessHandler.class);
    }

    @Override
    public List<ProcessPreListener> getPreProcessListeners() {
        return extensionManager.getExtensionList(preProcessListenersKey, ProcessPreListener.class);
    }

    @Override
    public List<ProcessPostListener> getPostProcessListeners() {
        return extensionManager.getExtensionList(postProcessListenersKey, ProcessPostListener.class);
    }

    @Override
    public ExtensionPoint getPreProcessListenersExtensionPoint() {
        return extensionManager.getExtensionPoint(preProcessListenersKey, ProcessPreListener.class);
    }

    @Override
    public ExtensionPoint getProcessHandlersExtensionPoint() {
        return extensionManager.getExtensionPoint(processHandlersKey, ProcessHandler.class);
    }

    @Override
    public ExtensionPoint getPostProcessListenersExtensionPoint() {
        return extensionManager.getExtensionPoint(postProcessListenersKey, ProcessPostListener.class);
    }

    @Override
    public String getName() {
        return name;
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

}
