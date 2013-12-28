package io.github.ibuildthecloud.dstack.process.common.generic;

import io.github.ibuildthecloud.dstack.engine.process.AbstractProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.StateTransition;
import io.github.ibuildthecloud.dstack.engine.process.impl.ResourceStatesDefinition;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class GenericResourceProcessDefinition extends AbstractProcessDefinition {

    String resourceType;
    ResourceStatesDefinition statesDefinition;
    ObjectManager objectManager;
    JsonMapper jsonMapper;

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        return new GenericResourceProcessState(jsonMapper, statesDefinition, config, objectManager);
    }

    @Override
    public Set<String> getHandlerRequiredResultData() {
        return this.statesDefinition.getRequiredFields();
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ResourceStatesDefinition getStatesDefinition() {
        return statesDefinition;
    }

    @Inject
    public void setStatesDefinition(ResourceStatesDefinition statesDefinition) {
        this.statesDefinition = statesDefinition;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Inject
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public List<StateTransition> getStateTransitions() {
        return statesDefinition.getStateTransitions();
    }

}
