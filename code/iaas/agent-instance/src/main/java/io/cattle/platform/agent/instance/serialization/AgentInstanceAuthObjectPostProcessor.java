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
        if (!(obj instanceof Instance)) {
            return;
        }

        Instance instance = (Instance) obj;

        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if (agent == null) {
            return;
        }

        Map<String, Object> auth = AgentUtils.getAgentAuth(agent, objectManager);
        if (auth != null) {
            Map<String, Object> fields = DataUtils.getWritableFields(data);
            for (Map.Entry<String, Object> entry : auth.entrySet()) {
                    DataAccessor.fromMap(fields)
                        .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT).withKey(entry.getKey()).set(entry.getValue());
            }
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
