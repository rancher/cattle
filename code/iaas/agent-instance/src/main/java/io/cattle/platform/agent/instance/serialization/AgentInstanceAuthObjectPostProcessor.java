package io.cattle.platform.agent.instance.serialization;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.Map;

import javax.inject.Inject;

public class AgentInstanceAuthObjectPostProcessor implements ObjectTypeSerializerPostProcessor {

    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE };
    }

    @Override
    public void process(Object obj, String type, Map<String, Object> data) {
        if ( ! ( obj instanceof Instance ) ) {
            return;
        }

        Instance instance = (Instance)obj;

        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if ( agent == null ) {
            return;
        }

        String auth = AgentUtils.getAgentAuth(agent, objectManager);
        if ( auth != null ) {
            DataAccessor.fromDataFieldOf(data)
                .withKey("agentInstanceAuth")
                .set(auth);

            Map<String,Object> fields = DataUtils.getWritableFields(data);
            DataAccessor.fromMap(fields)
                .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                .withKey("CATTLE_AGENT_INSTANCE_AUTH")
                .set(auth);
        }
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
