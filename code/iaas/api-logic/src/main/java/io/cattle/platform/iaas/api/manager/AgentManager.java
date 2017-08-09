package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashMap;
import java.util.Map;

public class AgentManager extends AbstractJooqResourceManager {
    @Override
    public String[] getTypes() {
        return new String[] { "agent" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Agent.class };
    }

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        if (!(obj instanceof Agent)) {
            return super.deleteInternal(type, id, obj, request);
        }

        try {
            return super.deleteInternal(type, id, obj, request);
        } catch (ClientVisibleException e) {
            if (ResponseCodes.METHOD_NOT_ALLOWED == e.getStatus()) {
                Map<String, Object> data = new HashMap<>();
                data.put("agent.deactivate" + ProcessLogic.CHAIN_PROCESS, "agent.remove");
                scheduleProcess("agent.deactivate", obj, data);
                scheduleProcess("agent.deactivate", obj, CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
                return getObjectManager().reload(obj);
            } else {
                throw e;
            }
        }
    }
}
