package io.cattle.platform.agent;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;

public class AgentRequest extends EventVO<Event> {

    public AgentRequest() {
    }

    public AgentRequest(Long agentId, Event event) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent id is null");
        }

        setName(IaasEvents.AGENT_REQUEST);
        setResourceId(agentId.toString());
        setResourceType(AgentConstants.TYPE);
        setData(event);
    }

}
