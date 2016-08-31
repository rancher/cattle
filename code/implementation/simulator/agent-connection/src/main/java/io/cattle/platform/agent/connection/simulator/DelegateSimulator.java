package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.iaas.event.delegate.DelegateEvent;

import java.util.Map;

public class DelegateSimulator implements Simulator {

    Simulator target;
    Map<String, Object> instanceData;

    public DelegateSimulator(Simulator target, Map<String, Object> instanceData) {
        super();
        this.target = target;
        this.instanceData = instanceData;
    }
    
    @Override
    public Event execute(Event event) {
        Event resp = target.execute(new DelegateEvent(instanceData, event));
        if (resp == null) {
            return null;
        }
        
        Event data = (Event)resp.getData();

        EventVO<?> reply = EventVO.reply(event).withData(data.getData());
        EventUtils.copyTransitioning(data, reply);
        return reply;
    }

}
