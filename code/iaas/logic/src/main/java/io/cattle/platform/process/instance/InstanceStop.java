package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.Arrays;

import javax.inject.Named;

@Named
public class InstanceStop extends AgentBasedProcessHandler {

    public InstanceStop() {
        setCommandName("compute.instance.deactivate");
        setDataTypeClass(Instance.class);
        setProcessNames(InstanceConstants.PROCESS_STOP);
        setShortCircuitIfAgentRemoved(true);
        setProcessDataKeys(Arrays.asList("timeout", "containerNoOpEvent"));
        setPriority(DEFAULT);
    }

}
