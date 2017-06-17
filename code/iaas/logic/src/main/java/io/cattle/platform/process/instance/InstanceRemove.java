package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class InstanceRemove extends AgentBasedProcessHandler {

    public InstanceRemove() {
        setCommandName("compute.instance.remove");
        setDataTypeClass(Instance.class);
        setProcessNames(InstanceConstants.PROCESS_REMOVE);
        setShortCircuitIfAgentRemoved(true);
        setPriority(DEFAULT);
    }

}
