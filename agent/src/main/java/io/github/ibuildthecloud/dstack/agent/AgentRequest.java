package io.github.ibuildthecloud.dstack.agent;

import io.github.ibuildthecloud.dstack.core.events.CoreEvents;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.util.type.TypeConstants;

public class AgentRequest extends EventVO {

    public AgentRequest() {
    }

    public AgentRequest(Long agentId, Event event) {
        if ( agentId == null ) {
            throw new IllegalArgumentException("Agent id is null");
        }

        setName(CoreEvents.AGENT_REQUEST);
        setResourceId(agentId.toString());
        setResourceType(TypeConstants.AGENT);
        setData(event);
    }

    @Override
    public EventVO getData() {
        return (EventVO)super.getData();
    }

    public void setData(EventVO data) {
        super.setData(data);
    }

}
