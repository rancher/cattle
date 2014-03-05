package io.github.ibuildthecloud.dstack.agent;

import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;
import io.github.ibuildthecloud.dstack.util.type.TypeConstants;

public class AgentRequest extends EventVO<Event> {

    public AgentRequest() {
    }

    public AgentRequest(Long agentId, Long groupId, Event event) {
        if ( agentId == null ) {
            throw new IllegalArgumentException("Agent id is null");
        }

        if ( groupId == null ) {
            setName(IaasEvents.AGENT_REQUEST);
        } else {
            setName(IaasEvents.appendAgentGroup(IaasEvents.AGENT_REQUEST, groupId));
        }

        setResourceId(agentId.toString());
        setResourceType(TypeConstants.AGENT);
        setData(event);
    }

}
