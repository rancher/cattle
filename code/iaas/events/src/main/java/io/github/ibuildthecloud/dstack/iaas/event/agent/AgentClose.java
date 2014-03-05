package io.github.ibuildthecloud.dstack.iaas.event.agent;

import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;

public class AgentClose extends EventVO<Object> {

    public AgentClose() {
        setName(IaasEvents.AGENT_CLOSE);
    }

}
