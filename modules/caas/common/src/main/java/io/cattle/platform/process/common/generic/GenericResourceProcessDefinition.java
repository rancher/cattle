package io.cattle.platform.process.common.generic;

import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.AbstractProcessDefinition;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessHandlerRegistry;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

public class GenericResourceProcessDefinition extends AbstractProcessDefinition {

    String resourceType;
    ResourceStatesDefinition statesDefinition;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    ProcessRecordDao processRecordDao;

    public GenericResourceProcessDefinition(String name, ProcessHandlerRegistry registry, String resourceType, ResourceStatesDefinition statesDefinition, ObjectManager objectManager, JsonMapper jsonMapper,
            ProcessRecordDao processRecordDao) {
        super(name, registry);
        this.resourceType = resourceType;
        this.statesDefinition = statesDefinition;
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.processRecordDao = processRecordDao;
    }

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        return new GenericResourceProcessState(jsonMapper, statesDefinition, config, objectManager, processRecordDao);
    }

    public ResourceStatesDefinition getStatesDefinition() {
        return statesDefinition;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public List<StateTransition> getStateTransitions() {
        return statesDefinition.getStateTransitions();
    }

}
