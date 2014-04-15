package io.cattle.platform.iaas.event.delegate;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;

import java.util.Map;

public class DelegateEvent extends EventVO<DelegateEventData>{

    public DelegateEvent() {
        setName(IaasEvents.DELEGATE_REQUEST);
    }

    public DelegateEvent(Map<String, Object> instanceData, Event event) {
        setName(IaasEvents.DELEGATE_REQUEST);
        setData(new DelegateEventData(instanceData, event));
    }

}
