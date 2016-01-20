package io.cattle.platform.process.common.generic;

import io.cattle.platform.engine.process.AbstractProcessDefinition;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

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
