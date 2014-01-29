package io.github.ibuildthecloud.dstack.agent;

import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;
import io.github.ibuildthecloud.dstack.util.type.TypeConstants;

public class AgentRequest extends EventVO<Event> {

    public AgentRequest() {
    }

    public AgentRequest(Long agentId, Event event) {
        if ( agentId == null ) {
            throw new IllegalArgumentException("Agent id is null");
        }

        setName(IaasEvents.AGENT_REQUEST);
        setResourceId(agentId.toString());
        setResourceType(TypeConstants.AGENT);
        setData(event);
    }

}
