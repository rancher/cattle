package io.cattle.platform.iaas.event.agent;

import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;

public class AgentClose extends EventVO<Object> {

    public AgentClose() {
        setName(IaasEvents.AGENT_CLOSE);
    }

}
