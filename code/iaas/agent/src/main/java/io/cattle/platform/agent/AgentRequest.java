package io.cattle.platform.agent;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.util.type.TypeConstants;

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
