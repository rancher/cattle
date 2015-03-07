package io.cattle.platform.extension.api.model;

import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.extension.ExtensionPoint;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "processDefinition")
public class ProcessDefinitionApi {

    boolean extensionBased;
    String name;
    ExtensionPoint preProcessListeners, processHandlers, postProcessListeners;
    String resourceType;
    List<StateTransition> stateTransitions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExtensionBased() {
        return extensionBased;
    }

    public void setExtensionBased(boolean extensionBased) {
        this.extensionBased = extensionBased;
    }

    public ExtensionPoint getPreProcessListeners() {
        return preProcessListeners;
    }

    public void setPreProcessListeners(ExtensionPoint preProcessListeners) {
        this.preProcessListeners = preProcessListeners;
    }

    public ExtensionPoint getProcessHandlers() {
        return processHandlers;
    }

    public void setProcessHandlers(ExtensionPoint processHandlers) {
        this.processHandlers = processHandlers;
    }

    public ExtensionPoint getPostProcessListeners() {
        return postProcessListeners;
    }

    public void setPostProcessListeners(ExtensionPoint postProcessListeners) {
        this.postProcessListeners = postProcessListeners;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public List<StateTransition> getStateTransitions() {
        return stateTransitions;
    }

    public void setStateTransitions(List<StateTransition> stateTransitions) {
        this.stateTransitions = stateTransitions;
    }

    public String getId() {
        return name;
    }

}
