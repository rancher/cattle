package io.cattle.platform.ha.monitor.event;

import io.cattle.platform.eventing.model.EventVO;

import java.util.HashMap;
import java.util.Map;

import static io.cattle.platform.core.constants.InstanceConstants.*;

public class InstanceForceStop extends EventVO<Map<String, Object>, Object> {

    public InstanceForceStop(String externalId) {
        setName(EVENT_INSTANCE_FORCE_STOP);
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> containerforceStop = new HashMap<String, Object>();
        containerforceStop.put("kind", "docker");
        containerforceStop.put("id", externalId);
        data.put("instanceForceStop", containerforceStop);
        setData(data);
    }
}
